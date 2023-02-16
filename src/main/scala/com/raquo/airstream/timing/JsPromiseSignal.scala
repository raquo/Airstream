package com.raquo.airstream.timing

import com.raquo.airstream.core.{Transaction, WritableSignal}

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

class JsPromiseSignal[A](promise: js.Promise[A]) extends WritableSignal[Option[A]] {

  override protected val topoRank: Int = 1

  private var promiseSubscribed: Boolean = false

  // #Note: It is not possible to synchronously get a Javascript promise's value,
  //  or even to check if it has been resolved, so we have to start this signal with None
  //  to avoid creating an infinite loop with our currentValueFromParent implementation.
  setCurrentValue(Success(None), isInitial = true)

  // #Note: We can't pull data from JS Promise on demand, this async access below is the best we can do.
  override protected def currentValueFromParent(): Try[Option[A]] = tryNow() // noop

  override protected def onWillStart(): Unit = {
    if (!promiseSubscribed) {
      promiseSubscribed = true
      promise.`then`[Unit](
        (nextValue: A) => {
          onPromiseResolved(Success(nextValue))
        },
        js.defined { (rawException: Any) => {
          val nextError = rawException match {
            case th: Throwable => th
            case _ => js.JavaScriptException(rawException)
          }
          onPromiseResolved(Failure(nextError))
        }}
      )
    }
  }

  private def onPromiseResolved(nextPromiseValue: Try[A]): Unit = {
    // #nc doc this about onWillStart
    // #Note Normally onWillStart must not create transactions / emit values, but this is ok here
    //  because this callback is always called asynchronously, so any value will be emitted from here
    //  long after the onWillStart / onStart chain has finished.
    // #Note fireTry sets current value even if the signal has no observers
    val nextValue = nextPromiseValue.map(Some(_))
    //println(s"> init trx from FutureSignal($value)")
    new Transaction(fireTry(nextValue, _)) // #Note[onStart,trx,async]
  }
}
