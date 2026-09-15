package com.raquo.airstream.web

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

final class FetchStreamLifecycleRegressionSpec extends UnitSpec {

  it("stopping a fetch unsubscribes from its cancellation source") {
    val previousFetch = js.Dynamic.global.globalThis.fetch
    val owner = ManualOwner()
    var abortSourceActive = false
    val abortSource = EventStream.fromCustomSource[Any](
      start = (_, _, _, _) => { abortSourceActive = true },
      stop = _ => { abortSourceActive = false }
    )

    // Keep the request pending. This test exercises subscriptions, not networking.
    js.Dynamic.global.globalThis.fetch = ((_: String, _: dom.RequestInit) => {
      js.Promise[dom.Response]((_, _) => ())
    }): js.Function2[String, dom.RequestInit, js.Promise[dom.Response]]

    try {
      val stream = FetchStream.get("https://example.invalid", _.abortStream(abortSource))
      stream.foreach(_ => ())(owner)
      assert(abortSourceActive)

      owner.killSubscriptions()
      assert(!abortSourceActive, "Cancellation source remains subscribed after the fetch stops")
    } finally {
      owner.killSubscriptions()
      js.Dynamic.global.globalThis.fetch = previousFetch
    }
  }

  it("restarting an abortOnStop fetch uses a non-aborted signal") {
    val previousFetch = js.Dynamic.global.globalThis.fetch
    val owner = ManualOwner()
    val requestSignals = mutable.Buffer.empty[dom.AbortSignal]
    val abortedAtRequest = mutable.Buffer.empty[Boolean]

    js.Dynamic.global.globalThis.fetch = ((_: String, init: dom.RequestInit) => {
      val signal = init.signal.get
      requestSignals += signal
      abortedAtRequest += signal.aborted
      js.Promise[dom.Response]((_, _) => ())
    }): js.Function2[String, dom.RequestInit, js.Promise[dom.Response]]

    try {
      val stream = FetchStream.get("https://example.invalid", _.abortOnStop())
      stream.foreach(_ => ())(owner)
      assert(abortedAtRequest.toList == List(false))

      owner.killSubscriptions()
      assert(requestSignals.head.aborted, "Stopping must actually abort the first request")

      stream.foreach(_ => ())(owner)
      assert(abortedAtRequest.toList == List(false, false))
      assert(requestSignals(0) ne requestSignals(1))
    } finally {
      owner.killSubscriptions()
      js.Dynamic.global.globalThis.fetch = previousFetch
    }
  }
}
