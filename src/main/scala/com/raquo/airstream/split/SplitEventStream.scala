package com.raquo.airstream.split

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentObservable}
import com.raquo.airstream.core.{EventStream, Protected, Signal, Transaction, WritableEventStream}

import scala.collection.mutable
import scala.util.Try

/** Broadly equivalent to `parent.map(_.map(project))`, but the `project` part
  * gets access to more data and is memoized by key.
  *
  * See docs.
  *
  * @param key       A sort of grouping / memoization key for inputs in `parent`
  * @param project   (key, initialInput, inputChangesForThisKey) => output
  *                  - Will only be called ONCE for a given key as long as parent contains an Input for this Key
  *                  - Updates to Input with this Key will be published in `inputChangesForThisKey`
  *                  - After parent stops containing an Input for this Key, we forget we ever called project for this key
  */
class SplitEventStream[M[_], Input, Output, Key](
  override protected[this] val parent: EventStream[M[Input]],
  key: Input => Key,
  project: (Key, Input, EventStream[Input]) => Output,
  splittable: Splittable[M]
) extends WritableEventStream[M[Output]] with SingleParentObservable[M[Input], M[Output]] with InternalNextErrorObserver[M[Input]] {

  override protected val topoRank: Int = Protected.topoRank(parent) + 1

  private[this] var memoized: Map[Key, (Input, Output)] = Map.empty

  protected override def onStop(): Unit = {
    memoized = Map.empty
    super.onStop()
  }

  override protected def onNext(nextInputs: M[Input], transaction: Transaction): Unit = {
    fireValue(memoizedProject(nextInputs), transaction)
  }

  override protected def onError(nextError: Throwable, transaction: Transaction): Unit = {
    fireError(nextError, transaction)
  }

  def toSignalWithInitialInput(lazyInitialInput: => Try[M[Input]]): Signal[M[Output]] = {
    new SignalFromEventStream[M[Output]](this, lazyInitialValue = lazyInitialInput.map(memoizedProject))
  }

  private[this] def memoizedProject(nextInputs: M[Input]): M[Output] = {
    val nextKeysDict = mutable.HashSet.empty[Key] // HashSet has desirable performance tradeoffs

    val nextOutputs = splittable.map(nextInputs, { (nextInput: Input) =>
      val memoizedKey = key(nextInput)
      nextKeysDict += memoizedKey

      val nextOutput = memoized.get(memoizedKey).fold(ifEmpty = {
        val initialInput = nextInput

        // @warning !!! DANGER ZONE !!!
        // @note: this comment is written for the the Signal use case.
        // - We must avoid mapping over this signal itself here to avoid infinite loop (this function calling `initialValue`)
        // - We must avoid looking at `memoized(key)` before `memoized` is populated with that key a few lines below
        // = Therefore, we look at a stream of changes populated with a known initial value

        // @TODO[Performance] I don't like this `collect`, can't we do this better?
        val inputStream = map(_ => memoized.get(memoizedKey).map(_._1)).collect { case Some(input) => input }
        val newOutput = project(memoizedKey, initialInput, inputStream)
        newOutput
      }){ memoizedTuple =>
        val memoizedOutput = memoizedTuple._2
        memoizedOutput
      }

      // Create initial record, or update `nextInput`
      memoized = memoized.updated(memoizedKey, (nextInput, nextOutput))

      nextOutput
    })

    memoized.keys.foreach { memoizedKey =>
      if (!nextKeysDict.contains(memoizedKey)) {
        memoized = memoized - memoizedKey
      }
    }

    nextOutputs
  }
}
