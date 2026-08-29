package com.raquo.airstream.custom

import com.raquo.airstream.core.{EventStream, Transaction, WritableStream}
import com.raquo.airstream.custom.CustomSource._
import com.raquo.airstream.util.FeatureFlags
import com.raquo.ew.JsArray

import scala.annotation.nowarn

/** Use this to easily create a custom signal from an external source
  *
  * See docs on custom sources, and [[CustomSource.Config]]
  */
class CustomStreamSource[A](
  makeConfig: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => CustomSource.Config,
) extends WritableStream[A] with CustomSource[A] {

  /** Events emitted synchronously while (re)starting are collected here (as
    * thunks) until `onStart` finishes, so [[resolveStartCallbacks]] can decide
    * how to schedule them. Asynchronously fired events bypass this, as they are never shared.
    */
  private[this] val onStartCallbacks: JsArray[Transaction => Unit] = JsArray()

  override protected[this] val config: Config = makeConfig(
    value => scheduleTransaction(fireValue(value, _)),
    err => scheduleTransaction(fireError(err, _)),
    () => startIndex,
    () => isStarted
  )

  override protected[this] def onStart(): Unit = {
    onStartCallbacks.length = 0 // should already be empty, but just in case
    super.onStart() // runs config.onStart(), which may fill onStartCallbacks
    resolveStartCallbacks()
  }

  /** Inside a shared start, collect the emission for [[resolveStartCallbacks]];
    * otherwise fire it in a new transaction right away, as usual.
    */
  private def scheduleTransaction(fire: Transaction => Unit): Unit = {
    if (
      Transaction.onStart.isInSharedStart &&
        (FeatureFlags.V18_TRX_ONSTART_FIX_144: @nowarn("msg=deprecated"))
    ) {
      onStartCallbacks.push(fire)
    } else {
      new Transaction(fire)
    }
  }

  /** Schedule the events emitted synchronously during `onStart`:
    *  - If exactly one event: emit it via the shared-start batch
    *    (`Transaction.onStart.add`), so it shares a transaction with siblings
    *    started in the same `onStart.shared` block (e.g. a Laminar mount), like
    *    `signal.updates` does. This lets `merge` / `combine` order simultaneous
    *    start-emissions deterministically, rather than by start order.
    *  - More than one: emit each in its own transaction (as before). An
    *    observable can't emit twice per transaction, and batching only the first
    *    would let the rest overtake it through a deferring `merge`.
    *
    * Gated behind [[FeatureFlags.V18_TRX_ONSTART_FIX_144]]; when off,
    * onStartCallbacks is always empty and this is a no-op.
    *
    * See https://github.com/raquo/Airstream/issues/144
    */
  private def resolveStartCallbacks(): Unit = {
    val numEvents = onStartCallbacks.length
    if (numEvents == 1) {
      val fire = onStartCallbacks(0)
      onStartCallbacks.length = 0
      Transaction.onStart.add { trx =>
        if (isStarted) {
          fire(trx)
        }
      }
    } else if (numEvents > 1) {
      var i = 0
      while (i < numEvents) {
        val fire = onStartCallbacks(i)
        new Transaction(fire)
        i += 1
      }
      onStartCallbacks.length = 0
    }
  }
}

object CustomStreamSource {

  @deprecated("Use EventStream.fromCustomSource", "15.0.0-M1")
  def apply[A](
    config: (FireValue[A], FireError, GetStartIndex, GetIsStarted) => Config
  ): EventStream[A] = {
    new CustomStreamSource[A](config)
  }
}
