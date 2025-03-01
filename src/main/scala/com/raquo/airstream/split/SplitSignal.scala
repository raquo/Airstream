package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.core.{AirstreamError, Protected, Signal, Transaction}
import com.raquo.airstream.timing.SyncDelayStream

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Try

/** Broadly similar to `parent.map(_.map(project))`, but the `project` part
  * gets access to more data and is memoized by key.
  *
  * See docs.
  *
  * @param key       A sort of grouping / memoization key for inputs in `parent`
  * @param distinctCompose   Transformation to apply to each key's input stream before providing it to `project`
  *                  - Usually you want `_.distinct` here, so that each of the streams is only triggered
  *                    when the input for its key actually changes (otherwise they would get an update
  *                    every time that the parent stream emitted)
  * @param project   (key, initialInput, inputChangesForThisKey) => output
  *                  - Will only be called ONCE for a given key as long as parent contains an Input for this Key
  *                  - Updates to Input with this Key will be published in `inputChangesForThisKey`
  *                  - After parent stops containing an Input for this Key, we forget we ever called project for this key
  */
class SplitSignal[M[_], Input, Output, Key](
  override protected[this] val parent: Signal[M[Input]],
  key: Input => Key,
  distinctCompose: Signal[Input] => Signal[Input],
  project: (Key, Input, Signal[Input]) => Output,
  splittable: Splittable[M],
  duplicateKeysConfig: DuplicateKeysConfig = DuplicateKeysConfig.default,
  strict: Boolean = false // #TODO `false` default for now to keep compatibility with 17.0.0 - consider changing in 18.0.0 #nc
) extends SingleParentSignal[M[Input], M[Output]] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def currentValueFromParent(): Try[M[Output]] = parent.tryNow().map(memoizedProject)

  // #TODO[Performance]
  //  - If we don't need Scala semantics for keys, then we can use JS map,
  //    which would likely be both smaller and faster than Scala Map
  //  - Keys are often primitives – strings or numbers – so this would
  //    likely help in most popular use cases.
  //  - However, don't bother until we can benchmark how much time we spend
  //    in memoization, and how much we'll gain from switching to JS Maps.
  /** key -> (inputValue, inputSignal, outputValue, lastParentUpdateId) */
  private[this] val memoized: mutable.Map[Key, (Input, Signal[Input], Output, Int)] = mutable.Map.empty

  override protected def onTry(nextParentValue: Try[M[Input]], transaction: Transaction): Unit = {
    super.onTry(nextParentValue, transaction)
    nextParentValue.fold(
      nextError => fireError(nextError, transaction),
      nextEvent => fireValue(memoizedProject(nextEvent), transaction)
    )
  }

  private[this] val sharedDelayedParent = new SyncDelayStream(parent, after = this)

  private[this] val emptyObserver = new InternalTryObserver[Input] {
    override protected def onTry(nextValue: Try[Input], transaction: Transaction): Unit = ()
  }

  private[this] def memoizedProject(nextInputs: M[Input]): M[Output] = {
    // Any keys not in this set by the end of this function will be removed from `memoized` map
    // This ensures that previously memoized values are forgotten once the source observables stops emitting their inputs
    val nextKeys = mutable.HashSet.empty[Key] // HashSet has desirable performance tradeoffs

    val duplicateKeys = if (duplicateKeysConfig.shouldWarn) js.Array[Key]() else null

    val nextOutputs = splittable.map(
      nextInputs,
      (nextInput: Input) => {
        val memoizedKey = key(nextInput)

        if (duplicateKeysConfig.shouldWarn && nextKeys.contains(memoizedKey)) {
          if (!duplicateKeys.contains(memoizedKey)) { // #Note: this uses scala == key comparison here, as desired
            duplicateKeys.push(memoizedKey)
          }
        }

        nextKeys += memoizedKey

        val cachedSignalAndOutput = memoized.get(memoizedKey).map(t => (t._2, t._3))

        val nextSignalAndOutput = cachedSignalAndOutput.getOrElse {
          val initialInput = nextInput

          // @warning !!! DANGER ZONE !!!
          // - We must avoid mapping over this signal itself here to avoid infinite loop (this function calling `initialValue`)
          // - We must avoid looking at `memoized.get(key)` before `memoized` is populated with that key a few lines below
          // = Therefore, we derive the child signal from the parent stream and a known initial value
          //   - Using this signal's own changes instead won't work, because if the user calls `addObserver` or `foreach`
          //     in the `project` callback, this will evaluate `initialValue`, causing an infinite loop.
          //   - @TODO[Integrity] Moreover, it seems that such an infinite loop won't be detected.
          //      Not sure why. I'm guessing must be one of our guards being excessive, but I can't find it.

          // Potential problem:
          // - calling `project` calls `inputSignal.foreach` in user code (e.g.)
          // - the result of `project` is needed to build output, to memoize it
          // - `inputSignal.foreach` in user code triggers `inputSignal.onAddedExternalObserver`
          // - that calls for `inputSignal.tryNow` to send the value to the new observer
          // - that calls `parent.tryNow.map(memoizedProject)`
          // - at this point, we still haven't obtained the output of `project` because we're still
          //   running inside of it
          // - so the code of memoizedProject goes into the same branch and into the `else` branch of `cachedOutput.getOrElse`
          // - which is where the flow started, so that's a loop
          // = I've been in this mess for so long, I forgot how exactly I fixed this. Tests will catch that if this happens again.

          // - `inputSignal` fetches the latest input from `memoized` and emits that, subject to `compose`,
          //   which by default applies the `distinct` operator to filter out changes to OTHER keys.
          // - Without this default, each inputSignal would receive updates whenever any other unrelated key
          //   was updated in the parent list of inputs. We do have a test to check that behaviour too.

          val inputSignal = distinctCompose(
            new SplitChildSignal[M, Input](
              sharedDelayedParent,
              initialValue = Some((initialInput, Protected.lastUpdateId(parent))),
              getMemoizedValue = () => {
                val maybeMemoizedValue = memoized.get(memoizedKey)
                (maybeMemoizedValue match {
                  case Some(memoizedValue) =>
                    val memoizedParentLastUpdateId = memoizedValue._4
                    if (Protected.lastUpdateId(parent) > memoizedParentLastUpdateId) {
                      // memoized storage does not have the latest data
                      //  - this can happen when individual ChildSplitSignal is active
                      //    while the SplitSignal is stopped (has no listeners)
                      //  - ChildSplitSignal depends on `memoized`, which is the internal
                      //    state of SplitSignal, but it does not actually depend on
                      //    SplitSignal itself (using an internal observer).
                      //  - So, in that case, SplitSignal's internal state is not updated,
                      //    and memoized contains stale data. To mitigate this, we detect
                      //    this situation using lastUpdateId check above, and if a discrepancy
                      //    is detected, we have SplitSignal pull fresh data before reading it again.
                      //  – I am not sure if setting up an actual dependency (w/ observer)
                      //    is a good idea. I tried making sharedDelayedParent depend
                      //    on SplitSignal in addition to `parent`, but that produces weird results.
                      currentValueFromParent() // pull fresh data
                      memoized.get(memoizedKey) // read the latest data
                    } else {
                      maybeMemoizedValue
                    }
                  case _ =>
                    None
                }).map(t => (t._1, t._4))
              }
            )
          )

          if (isStarted && strict) {
            inputSignal.addInternalObserver(emptyObserver, shouldCallMaybeWillStart = true)
          }

          val newOutput = project(memoizedKey, initialInput, inputSignal)

          (inputSignal, newOutput)
        }

        val inputSignal = nextSignalAndOutput._1
        val nextOutput = nextSignalAndOutput._2

        // Cache this key for the first time, or update the input so that inputSignal can fetch it
        // dom.console.log(s"${this} memoized.update ${memoizedKey} -> ${nextInput}")
        memoized.update(memoizedKey, (nextInput, inputSignal, nextOutput, Protected.lastUpdateId(parent)))

        nextOutput
      }
    )

    memoized.keys.foreach { memoizedKey =>
      if (!nextKeys.contains(memoizedKey)) {
        // dom.console.log(s"${this} memoized.remove ${memoizedKey}")
        if (strict) {
          val inputSignal = memoized(memoizedKey)._2
          inputSignal.removeInternalObserver(emptyObserver)
        }
        memoized.remove(memoizedKey)
      }
    }

    if (duplicateKeysConfig.shouldWarn && duplicateKeys.nonEmpty) {
      AirstreamError.sendUnhandledError(
        new Exception(s"Duplicate keys detected in {$parent}.split(): `${duplicateKeys.mkString("`, `")}`. This is a bug in your code. See comment in Airstream's DuplicateKeysConfig.scala.")
      )
    }

    nextOutputs
  }

  override protected[this] def onStart(): Unit = {
    if (strict) {
      parent.tryNow().foreach { inputs =>
        // splittable.foreach is perhaps less efficient that memoized.keys,
        // but it has a predictable order that users expect.
        splittable.foreach(inputs, (input: Input) => {
          val memoizedKey = key(input)
          val maybeInputSignal = memoized.get(memoizedKey).map(_._2)
          maybeInputSignal.foreach { inputSignal =>
            // - If inputSignal not found because `parent.tryNow()` and `memoized`
            //   are temporarily out of sync, it should be added later just fine
            //   when they sync up.
            // - Typical pattern is to call Protected.maybeWillStart(inputSignal) in onWillStart,
            //   but our SplitSignal does not actually depend on these inputSignal-s, so I think
            //   it's ok to do both onWillStart and onStart for them here.
            inputSignal.addInternalObserver(emptyObserver, shouldCallMaybeWillStart = true)
          }
        })
      }
    }
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    if (strict) {
      // memoized.keys has no defined order, so we don't want to
      // use it for starting subscriptions, but for stopping them,
      // seems fine.
      // This way we make sure to remove observers from exactly
      // all items that are in `memoized`, no more, no less.
      memoized.keys.foreach { memoizedKey =>
        val inputSignal = memoized(memoizedKey)._2
        inputSignal.removeInternalObserver(emptyObserver)
      }
    }
    super.onStop()
  }
}
