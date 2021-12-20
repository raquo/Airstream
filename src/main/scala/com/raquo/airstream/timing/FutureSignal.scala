package com.raquo.airstream.timing

import com.raquo.airstream.core.{Transaction, WritableSignal}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

// #nc[remove]
class FutureSignal[A](future: Future[A])(implicit ec: ExecutionContext) extends WritableSignal[Option[A]] {

  override protected val topoRank: Int = 1

  private var futureSubscribed = false

  override protected def currentValueFromParent(): Try[Option[A]] = {
    future.value match {
      case Some(value) => value.map(Some(_))
      case None => Success(None)
    }
  }

  override protected def onWillStart(): Unit = {
    // #TODO[sync] Not sure if we need this line or not.
    setCurrentValue(currentValueFromParent())

    if (!futureSubscribed && !future.isCompleted) {
      futureSubscribed = true
      // #Note onWillStart must not create transactions / emit values, but this is ok here
      //  because onComplete is always asynchronous in ScalaJS, so any value will be emitted
      //  long after the onWillStart / onStart chain has finished.
      // #Note fireTry sets current value even if the signal has no observers
      future.onComplete { value =>
        val nextValue = value.map(Some(_))
        if (nextValue.map(_.map(_ => ())) != tryNow().map(_.map(_ => ()))) {
          // #TODO[sync] If somehow the signal's current value has already been updated with the Future's resolved value,
          //  we don't want to emit a separate event. The `_.map(_ => ())` trick is just to avoid comparing the resolved
          //  values using `==` – that could be expensive, and it's not necessary since we know that a resolved Future
          //  can never change its value.
          //  I'm not actually sure if this condition is necessary, it would have to be some weird timing.
          //println(s"> init trx from FutureSignal($value)")
          new Transaction(fireTry(nextValue, _)) // #Note[onStart,trx,async]
        }
      }(ec)
    }
  }
}
