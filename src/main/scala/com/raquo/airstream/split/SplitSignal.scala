package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalTryObserver, SingleParentObservable}
import com.raquo.airstream.core.{Protected, Signal, Transaction, WritableSignal}
import com.raquo.airstream.timing.SyncDelayEventStream

import scala.collection.mutable
import scala.util.Try

// @TODO[Performance] Should this be SplitSignal, or SplitEventStream?
//  - I feel like SplitEventStream might be more efficient
//  - It could also be less problematic
//  - Maybe it won't require Splittable to have `empty`, thus working nicer with `splitOne`?

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
  splittable: Splittable[M]
) extends WritableSignal[M[Output]] with SingleParentObservable[M[Input], M[Output]] with InternalTryObserver[M[Input]] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  override protected def initialValue: Try[M[Output]] = parent.tryNow().map(memoizedProject)

  private[this] val memoized: mutable.Map[Key, (Input, Output)] = mutable.Map.empty

  override protected def onTry(nextValue: Try[M[Input]], transaction: Transaction): Unit = {
    nextValue.fold(
      nextError => fireError(nextError, transaction),
      nextEvent => fireValue(memoizedProject(nextEvent), transaction)
    )
  }

  private[this] val sharedDelayedParent = new SyncDelayEventStream(parent, this)

  private[this] def memoizedProject(nextInputs: M[Input]): M[Output] = {
    // Any keys not in this set by the end of this function will be removed from `memoized` map
    // This ensures that previously memoized values are forgotten once the source observables stops emitting their inputs
    val nextKeysDict = mutable.HashSet.empty[Key] // HashSet has desirable performance tradeoffs

    val nextOutputs = splittable.map(nextInputs, { (nextInput: Input) =>
      val memoizedKey = key(nextInput)
      nextKeysDict += memoizedKey

      val cachedOutput = memoized.get(memoizedKey).map(_._2)

      val nextOutput = cachedOutput.getOrElse {
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
            initialInput,
            () => memoized.get(memoizedKey).map(_._1)
          )
        )

        val newOutput = project(memoizedKey, initialInput, inputSignal)

        newOutput
      }

      // Cache this key for the first time, or update the input so that inputSignal can fetch it
      memoized.update(memoizedKey, (nextInput, nextOutput))

      nextOutput
    })

    memoized.keys.foreach { memoizedKey =>
      if (!nextKeysDict.contains(memoizedKey)) {
        memoized.remove(memoizedKey)
      }
    }

    nextOutputs
  }
}
