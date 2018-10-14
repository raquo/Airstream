package com.raquo.airstream.eventstream

import com.raquo.airstream.features.{CombineObservable, InternalParentObserver}

import scala.util.Try

/** Stream that combines the latest values from two streams into a tuple.
  * Only fires after both streams have sent a value.
  *
  * Note: this stream forgets any previous values of parent streams once it's stopped
  *
  * @param combinator Note: Must not throw. Must have no side effects. Can be executed more than once per transaction.
  */
class CombineEventStream2[A, B, O](
  parent1: EventStream[A],
  parent2: EventStream[B],
  combinator: (Try[A], Try[B]) => Try[O]
) extends EventStream[O] with CombineObservable[O] {

  override protected[airstream] val topoRank: Int = (parent1.topoRank max parent2.topoRank) + 1

  private[this] var maybeLastParent1Value: Option[Try[A]] = None
  private[this] var maybeLastParent2Value: Option[Try[B]] = None

  parentObservers.push(
    InternalParentObserver.fromTry[A](parent1, (nextParent1Value, transaction) => {
      // println(s"> updated p1 value to $nextParent1Value")
      maybeLastParent1Value = Some(nextParent1Value)
      maybeLastParent2Value.foreach { lastParent2Value =>
        internalObserver.onTry(combinator(nextParent1Value, lastParent2Value), transaction)
      }
    }),
    InternalParentObserver.fromTry[B](parent2, (nextParent2Value, transaction) => {
      // println(s"> updated p1 value to $nextParent1Value")
      maybeLastParent2Value = Some(nextParent2Value)
      maybeLastParent1Value.foreach { lastParent1Value =>
        internalObserver.onTry(combinator(lastParent1Value, nextParent2Value), transaction)
      }
    })
  )

  override protected[this] def onStop(): Unit = {
    maybeLastParent1Value = None
    maybeLastParent2Value = None
    super.onStop()
  }

}
