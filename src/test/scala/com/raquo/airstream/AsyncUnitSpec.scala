package com.raquo.airstream

import org.scalatest.funspec.AsyncFunSpec

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.util.Try

abstract class AsyncUnitSpec extends AsyncFunSpec with Matchers {

  implicit override def executionContext: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def delay[V](value: => V): Future[V] = {
    delay(0)(value)
  }

  def delay[V](millis: Int)(value: => V): Future[V] = {
    val promise = Promise[V]()
    js.timers.setTimeout(millis.toDouble) {
      promise.complete(Try(value))
    }
    promise.future.map(identity)(executionContext)
  }
}
