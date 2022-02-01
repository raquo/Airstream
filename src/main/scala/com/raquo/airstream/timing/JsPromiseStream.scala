package com.raquo.airstream.timing

import com.raquo.airstream.core.{Transaction, WritableStream}

import scala.scalajs.js
import scala.scalajs.js.|

/** This stream emits a value that the promise resolves with, even if the promise
  * was already resolved.
  *
  * This stream emits only once. If you want to remember the value, Use [[JsPromiseSignal]] instead.
  *
  * @param promise Note: guarded against failures
  */
class JsPromiseStream[A](promise: js.Promise[A], emitOnce: Boolean) extends WritableStream[A] {

  override protected val topoRank: Int = 1

  // #Note: It is not possible to synchronously check if a Javascript promise has been resolved or not,
  //  so we can't base our logic on whether the promise was resolved or not at a particular time.

  private var shouldSubscribe: Boolean = true

  private var isPending: Boolean = false

  override protected def onWillStart(): Unit = {
    if (shouldSubscribe && !isPending) {
      if (emitOnce) {
        shouldSubscribe = false
      }
      isPending = true
      promise.`then`[Unit](
        (nextValue: A) => {
          isPending = false
          //println(s"> init trx from FutureEventStream.init($nextValue)")
          new Transaction(fireValue(nextValue, _))
          (): Unit | js.Thenable[Unit]
        },
        js.defined { (rawException: Any) => {
          isPending = false
          val nextError = rawException match {
            case th: Throwable => th
            case _ => js.JavaScriptException(rawException)
          }
          //println(s"> init trx from JsPromiseEventStream.init($nextError)")
          new Transaction(fireError(nextError, _))
          (): Unit | js.Thenable[Unit]
        }}
      )
    }
  }
}
