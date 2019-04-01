package com.raquo.airstream.signal

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.features.{InternalTryObserver, SingleParentObservable}

import scala.collection.{immutable, mutable}
import scala.util.Try

// Caution: this "works on my machine" but is not quite polished yet.

// @TODO Does this need to be a Signal? Can we have a similar EventStream for better performance? To avoid extra comparisons, since diffing will take care of that
// @TODO[API] Switch type param order (Out vs Key) to match value params?
// @TODO[Docs]
// @TODO[Test]
class MemoizedCollection[Model, Out, Key](
  models: Signal[immutable.Seq[Model]],
  memoizeKey: Model => Key,
  renderModel: (Key, Signal[Model]) => Out
) extends Signal[immutable.Seq[Out]] with SingleParentObservable[immutable.Seq[Model], immutable.Seq[Out]] with InternalTryObserver[immutable.Seq[Model]] {

  override protected[this] val parent: Signal[immutable.Seq[Model]] = models

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  private[this] var memoized: Map[Key, (Model, Out)] = Map.empty

  // @TODO[API] Should `sharedChanges` look at this signal, or at parent? Timing would probably be slightly different (even though still synchronous)

  /** Shared for efficiency */
  private[this] val sharedChanges = changes

  protected override def onStop(): Unit = {
    memoized = Map.empty
    super.onStop()
  }

  override protected[airstream] def onTry(nextValue: Try[immutable.Seq[Model]], transaction: Transaction): Unit = {
    fireTry(project(nextValue), transaction)
  }

  override protected def initialValue: Try[immutable.Seq[Out]] = {
    project(parent.tryNow())
  }

  def project(nextValue: Try[immutable.Seq[Model]]): Try[immutable.Seq[Out]] = {
    nextValue.map { nextModels =>
      var nextKeysDict = mutable.HashSet.empty[Key] // HashSet has desirable performance tradeoffs

      val nextOut = nextModels.map { nextModel =>
        val key = memoizeKey(nextModel)
        nextKeysDict += key

        memoized.get(key).fold(ifEmpty = {
          // !!! DANGER ZONE !!!
          // - We must avoid looking mapping over this signal itself here to avoid infinite loop (this function calling `initialValue`)
          // - We must avoid looking at `memoized(key)` before `memoized` is populated with that key a couple lines below
          // = Therefore, we look at a stream of changes populated with a known initial value
          val modelSignal = sharedChanges.map(_ => memoized(key)._1).toSignal(nextModel)
          val newOut = renderModel(key, modelSignal)
          memoized = memoized.updated(key, (nextModel, newOut))
          newOut
        })(_._2)
      }

      memoized.keys.foreach { memoizedKey =>
        if (!nextKeysDict.contains(memoizedKey)) {
          memoized = memoized - memoizedKey
        }
      }

      nextOut
    }
  }
}
