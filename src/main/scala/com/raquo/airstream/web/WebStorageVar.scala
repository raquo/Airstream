package com.raquo.airstream.web

import com.raquo.airstream.core.{BaseObservable, EventStream, Observer, Signal}
import com.raquo.airstream.ownership.{ManualOwner, Owner}
import com.raquo.airstream.state.SourceVar
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Success, Try}

/** WebStorageVar provides a Var with persistence powered
  * by the browser's LocalStorage or SessionStorage API.
  *
  * It's scoped to one key in LocalStorage or SessionStorage.
  *
  * #Warning: don't create multiple vars for the same key in
  *  the same browser tab / frame, they will go out of sync.
  *  - To sync vars pointing to the same key across different
  *    browser tabs / frames, see `syncOwner` param of
  *    `WebStorageVar.localStorage/sessionStorage` factories.
  *
  * Note: if you push an error value into this Var, the
  * underlying browser storage will NOT be updated, and the
  * error value will NOT sync to other tabs / frames.
  *
  * Note: Users may have disabled site data (incl. LocalStorage
  * and SessionStorage) in browser settings. In those cases, the
  * Var will still work as a normal Airstream Var, but its value
  * will not be persisted in the browser.
  *  - To detect this condition, see helpers on the companion object.
  *
  * See https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
  * See https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage
  *
  * For constructor params doc, see scaladoc for:
  *  - [[WebStorageVar.localStorage]]
  *  - [[WebStorageVar.sessionStorage]]
  */
class WebStorageVar[A] private[web] (
  private[web] val maybeStorage: () => Option[dom.Storage],
  private[web] val key: String,
  encode: A => String,
  decode: String => Try[A],
  default: => Try[A],
  syncDistinctByFn: (A, A) => Boolean
) extends SourceVar[A](
  initial = maybeStorage().flatMap(s => Option(s.getItem(key)).map(decode)).getOrElse(default)
) {

  private[web] val owner = new ManualOwner

  /** Stream of updates of this sessionStorage/localStorage value
    * from other browser tabs and frames that have shared access to it.
    */
  lazy val externalUpdates: EventStream[dom.StorageEvent] =
    DomEventStream[dom.StorageEvent](
      eventTarget = dom.window,
      eventKey = "storage"
    ).filter { ev =>
      // SessionStorage is not shared between tabs, but storage events can still
      // be triggered between same-origin frames in the same tab, so we do need to
      // filter by storageArea.
      maybeStorage().contains(ev.storageArea) && ev.key == key
    }

  /** Signal of raw values in the browser storage.
    * Note: Updates are triggered only when this Var is updated.
    */
  lazy val rawStorageValues: Signal[Option[String]] =
    signal.mapTo(maybeStorage().flatMap(s => Option(s.getItem(key))))

  // When user writes to this var, update the value in web storage
  // #Note we filter out duplicate values not here, but when we
  //  pull/sync this var. Here we only look at non-error values,
  //  so we should have no cross-tab syncing loops.
  signal.addObserver(Observer.withRecover[A](
    onNext = { newValue =>
      maybeStorage().foreach { storage =>
        val newValueStr = encode(newValue)
        try {
          storage.setItem(key, newValueStr)
        } catch {
          // In some browsers in some cases, storage may not allow
          // writes even if the storage object is available.
          // See webStorageError method at the bottom of the file.
          // #TODO[API] Should we report this error somehow?
          case _: js.JavaScriptException => ()
        }
      }
    },
    onError = {
      // If this observer is the only observer on `signal`, we want to send
      // this error to unhandled. This is done by default in Observer, so we
      // just ignore the error if the signal has more than one observer.
      // This logic is intended to "fix" the `isError` logic that we have in
      // `WritableSignal.fireTry` to avoid both over- and under-reporting of
      // unhandled errors in this Var. The amended logic should have the same
      // outcome (w.r.t. reporting) as if this signal.addObserver did not exist.
      case _: Throwable if BaseObservable.numAllObservers(signal) > 1 => () // don't report in this case
    },
    handleObserverErrors = false
  ))(owner)

  /** Local storage is shared between all browser tabs and frames of the same origin.
    * Session storage is shared between frames of the same origin in the current tab.
    *
    * Calling this method lets you listen to local/session storage updates from
    * all other sources that have shared access to it, and sync the updated values
    * into this Var.
    *
    * You also achieve this by passing `syncOwner` when creating this var.
    *
    * Note: if you want the storage Var-s in two tabs to sync, you need
    * to enable syncing for both of them.
    *
    * You can also [[pullOnce]] if you don't need continuous updates.
    */
  def syncFromExternalUpdates(implicit syncOwner: Owner): Unit = {
    // When we first enable sync, update this Var's value once,
    // if the stored value has changed since we've last read it.
    pullOnce(distinctOnly = true)
    // Keep this var's value up-to-date
    externalUpdates.foreach { ev =>
      setFromStoredValue(Option(ev.newValue), distinctOnly = true)
    }(syncOwner)
  }

  /** Manually update this var from the current value in storage. */
  def pullOnce(distinctOnly: Boolean = false): Unit = {
    maybeStorage() match {
      case Some(storage) =>
        setFromStoredValue(
          storedValueOpt = Option(storage.getItem(key)),
          distinctOnly
        )
      case None =>
        ()
    }
  }

  /** Update this Var's value by manually providing the raw value from
    * localStorage / sessionStorage.
    *
    * @param storedValueOpt if None is provided, `default` value will be used.
    * @param distinctOnly   if true, `syncDistinctByFn` will be applied as a filter.
    */
  def setFromStoredValue(
    storedValueOpt: Option[String],
    distinctOnly: Boolean = false
  ): Unit = {
    // #TODO[API] Should we also distinct error values?
    val decodedValueTry = storedValueOpt.fold(ifEmpty = default)(decode)
    lazy val isSame = (decodedValueTry, tryNow()) match {
      case (Success(decodedValue), Success(varValue)) =>
        syncDistinctByFn(decodedValue, varValue) // returns isSame
      case _ =>
        // #Note All errors are considered distinct.
        //  This should be enough to prevent infinite loops when syncing this Var
        //  because
        false
    }
    if (!(distinctOnly && isSame)) {
      setTry(decodedValueTry)
    }
  }

  override protected def defaultDisplayName: String = super.defaultDisplayName + s"(key=$key)"
}

object WebStorageVar {

  /** Start building a local storage Var.
    * Call `.text(...)` or `.withCodec(...)` or other helpers to get the Var from the result.
    *
    * #Warning: You must not use more than one Var for each LocalStorage key at the same
    *  time (in the same browser tab / frame), as they will not know about each other's updates.
    *  - They can only learn about updates from OTHER tabs / frames, if you specify syncOwner.
    *    #TODO If needed, I think we can improve on this by creating our own shared bus of updates.
    *
    * @param key       LocalStorage key
    * @param syncOwner If you provide an owner, this Var will listen to local storage
    *                  updates coming from other browser tabs (local storage is shared
    *                  across all open tabs on the same origin) while the owner is active.
    *                   - You can pass `unsafeWindowOwner` if you're ok with this var being
    *                     garbage collected only when you close this browser tab.
    *                     - This is usually ok, except when you're creating ephemeral
    *                       instances of LocalStorageVar (e.g. one per item in a dynamic
    *                       list) – in such cases, use one of Laminar's helpers to get an
    *                       owner scoped to that one item, so that it's garbage collected
    *                       when the corresponding element is unmounted.
    *                   - If you pass `None`, LocalStorageVar won't be able to update
    *                     its value when other tabs update local storage.
    *                     This may be ok for single-tab use cases.
    *                   - See also scaladoc for [[WebStorageVar.syncFromExternalUpdates]]
    * @see https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
    */
  def localStorage(
    key: String,
    syncOwner: Option[Owner]
  ): WebStorageBuilder = {
    val maybeStorage = () => Try(dom.window.localStorage).toOption
    new WebStorageBuilder(maybeStorage, key, syncOwner)
  }

  /** Start building a session storage Var.
    * Call `.text(...)` or `.withCodec(...)` or other helpers to get the Var from the result.
    *
    * #Warning: You must not use more than one Var for each SessionStorage key at the same time.
    *
    * @param key       SessionStorage key
    * @param syncOwner See docs for [[localStorage]] – this works similarly, except for
    *                  cross-frame syncing within the same-origin frames of the current
    *                  browser tab, corresponding to the scope of the underlying
    *                  SessionStorage.
    * @see https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage
    */
  def sessionStorage(
    key: String,
    syncOwner: Option[Owner]
  ): WebStorageBuilder = {
    val maybeStorage = () => Try(dom.window.sessionStorage).toOption
    new WebStorageBuilder(maybeStorage, key, syncOwner)
  }

  /** Users may disable site data (incl. LocalStorage) in browser settings.
    * This checks whether you can actually write to LocalStorage.
    *  - Note: If this returns false, Airstream's local storage Var would keep
    *    working as a regular Var, but without persistence to localStorage.
    *
    * See [[localStorageError]]
    */
  def isLocalStorageAvailable(): Boolean = localStorageError().isEmpty

  /** Users may disable site data (incl. SessionStorage) in browser settings.
    * This checks whether you can actually write to SessionStorage.
    *  - Note: If this returns false, Airstream's session storage Var would keep
    *    working as a regular Var, but without persistence to sessionStorage.
    *
    * See [[sessionStorageError]]
    */
  def isSessionStorageAvailable(): Boolean = sessionStorageError().isEmpty

  /** If this method returns an error, you won't be able to save data to LocalStorage.
    *  - Note: the Airstream LocalStorage Var will continue working as an ephemeral Var.
    *
    * See [[isLocalStorageAvailable]]
    */
  def localStorageError(): Option[dom.DOMException] = {
    webStorageError(Try(dom.window.localStorage))
  }

  /** If this method returns an error, you won't be able to save data to SessionStorage,
    *  - Note: the Airstream SessionStorage Var will continue working as an ephemeral Var.
    *
    * See [[isSessionStorageAvailable]]
    */
  def sessionStorageError(): Option[dom.DOMException] = {
    webStorageError(Try(dom.window.sessionStorage))
  }

  private val testKey = s"test_16417602047200375"

  private def webStorageError(storage: Try[dom.Storage]): Option[dom.DOMException] = {
    // https://stackoverflow.com/a/16427747/2601788
    storage.map { s =>
      s.setItem(testKey, "test")
      s.removeItem(testKey)
    }.fold(
      {
        case err: js.JavaScriptException =>
          // I'm pretty sure that we can only get DOMException here, but if we
          // get a different kind of JS exception, we're probably better off
          // pretending that it's a DOMException – their public members
          // (name and message) should be the same anyway.
          Some(err.exception.asInstanceOf[dom.DOMException])
        case err => throw err // Should not happen. Report bug if you see this.
      },
      _ => None
    )
  }

}
