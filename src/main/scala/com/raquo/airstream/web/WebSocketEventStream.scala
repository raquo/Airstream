package com.raquo.airstream.web

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}
import com.raquo.airstream.web.WebSocketEventStream.Transmitter
import org.scalajs.dom

import scala.scalajs.js

/**
  * [[WebSocketEventStream]] emits messages from a [[dom.WebSocket]] connection.
  *
  * Lifecycle:
  *  - A new connection is established when this stream is started.
  *  - Upstream messages, if any, are transmitted on this connection.
  *  - Server [[dom.MessageEvent messages]] and connection [[DomError errors]] are propagated downstream.
  *  - The connection is closed when this stream is stopped.
  */
class WebSocketEventStream[A](override val parent: EventStream[A], url: String)(implicit T: Transmitter[A])
  extends EventStream[dom.MessageEvent]
    with SingleParentObservable[A, dom.MessageEvent]
    with InternalNextErrorObserver[A] {

  protected[airstream] val topoRank: Int = 1

  private var jsSocket: js.UndefOr[dom.WebSocket] = js.undefined

  protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    // noop
  }

  protected[airstream] def onNext(nextValue: A, transaction: Transaction): Unit = {
    // transmit upstream message, no guard required since transmitter is trusted
    jsSocket.foreach(T.transmit(_, nextValue))
  }

  override protected[this] def onStart(): Unit = {

    val socket = new dom.WebSocket(url)

    // initialize new socket
    T.initialize(socket)

    // propagate connection termination error
    socket.onclose =
      (e: dom.CloseEvent) => if (jsSocket.nonEmpty) {
        jsSocket = js.undefined
        new Transaction(fireError(DomError(e), _))
      }

    // propagate connection error
    socket.onerror =
      (e: dom.Event) => if (jsSocket.nonEmpty) new Transaction(fireError(DomError(e), _))

    // propagate message received
    socket.onmessage =
      (e: dom.MessageEvent) => if (jsSocket.nonEmpty) new Transaction(fireValue(e, _))

    // update local reference
    socket.onopen =
      (_: dom.Event) => if (jsSocket.isEmpty) jsSocket = socket

    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    // Is "close" async?
    // just to be safe, reset local reference before closing to prevent error propagation in "onclose"
    val socket = jsSocket
    jsSocket = js.undefined
    socket.foreach(_.close())
    super.onStop()
  }
}

object WebSocketEventStream {

  /**
    * Returns an [[EventStream]] that emits [[dom.MessageEvent messages]] from a [[dom.WebSocket]] connection.
    *
    * Websocket [[dom.Event errors]], including [[dom.CloseEvent termination]], are propagated as [[DomError]]s.
    *
    * @param url '''absolute''' URL of the websocket endpoint,
    *            use [[websocketUrl]] to construct an absolute URL from a relative one
    * @param transmit   stream of messages to be transmitted to the websocket endpoint
    */
  def apply[A: Transmitter](url: String, transmit: EventStream[A] = EventStream.empty): EventStream[dom.MessageEvent] =
    new WebSocketEventStream(transmit, url)

  sealed abstract class Transmitter[A] {

    def initialize(socket: dom.WebSocket): Unit
    def transmit(socket: dom.WebSocket, data: A): Unit
  }

  object Transmitter {

    private def binary[A](send: (dom.WebSocket, A) => Unit, binaryType: String): Transmitter[A] =
      new Transmitter[A] {
        final def initialize(socket: dom.WebSocket): Unit         = socket.binaryType = binaryType
        final def transmit(socket: dom.WebSocket, data: A): Unit  = send(socket, data)
      }

    private def simple[A](send: (dom.WebSocket, A) => Unit): Transmitter[A] =
      new Transmitter[A] {
        final def initialize(socket: dom.WebSocket): Unit         = ()
        final def transmit(socket: dom.WebSocket, data: A): Unit  = send(socket, data)
      }

    implicit val binaryTransmitter: Transmitter[js.typedarray.ArrayBuffer]  = binary(_ send _, "arraybuffer")
    implicit val blobTransmitter: Transmitter[dom.Blob]                     = binary(_ send _, "blob")
    implicit val stringTransmitter: Transmitter[String]                     = simple(_ send _)
  }
}
