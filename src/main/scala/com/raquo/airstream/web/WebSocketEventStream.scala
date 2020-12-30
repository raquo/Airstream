package com.raquo.airstream.web

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.{InternalNextErrorObserver, SingleParentObservable}
import com.raquo.airstream.web.WebSocketEventStream.{Driver, WebSocketClosed, WebSocketError}
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Success, Try}

/**
  * An event source that emits messages from a [[dom.WebSocket]] connection.
  *
  * Stream lifecycle:
  *  - A new websocket connection is established on start.
  *  - Outgoing messages, if any, are sent on this connection.
  *    - Transmission failures, due to connection termination, are propagated as errors.
  *  - Connection termination, not initiated by this stream, is propagated as an error.
  *  - Incoming messages are propagated as events.
  *  - The connection is closed on stop.
  */
class WebSocketEventStream[I, O] private(
  override val parent: EventStream[I],
  project: dom.MessageEvent => Try[O],
  url: String
)(implicit D: Driver[I]) extends EventStream[O] with SingleParentObservable[I, O] with InternalNextErrorObserver[I] {

  protected[airstream] val topoRank: Int = 1

  private var jsSocket: js.UndefOr[dom.WebSocket] = js.undefined

  protected[airstream] def onError(nextError: Throwable, transaction: Transaction): Unit = {
    // noop
  }

  protected[airstream] def onNext(nextValue: I, transaction: Transaction): Unit = {
    // transmit upstream message, no guard required since driver is trusted
    jsSocket.fold(fireError(WebSocketError(nextValue), transaction))(D.transmit(_, nextValue))
  }

  override protected[this] def onStart(): Unit = {

    val socket = new dom.WebSocket(url)

    // initialize new socket
    D.initialize(socket)

    // register callbacks and update local reference
    socket.onopen =
      (_: dom.Event) => if (jsSocket.isEmpty) {

        // https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_client_applications#Creating_a_WebSocket_object
        // as per documentation, "onclose" is called right after "onerror"
        // so register callback for "onclose" only

        // propagate connection close event as error
        socket.onclose =
          (e: dom.CloseEvent) => if (jsSocket.nonEmpty) {
            jsSocket = js.undefined
            new Transaction(fireError(WebSocketClosed(e), _))
          }

        // propagate message received
        socket.onmessage =
          (e: dom.MessageEvent) => if (jsSocket.nonEmpty) {
            project(e).fold(e => new Transaction(fireError(e, _)), o => new Transaction(fireValue(o, _)))
          }

        jsSocket = socket
      }

    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    // Is "close" async?
    // just to be safe, reset local reference before closing to prevent error propagation in "onclose" callback
    val socket = jsSocket
    jsSocket = js.undefined
    socket.foreach(_.close())
    super.onStop()
  }
}

object WebSocketEventStream {

  sealed abstract class WebSocketStreamException extends Exception
  final case class WebSocketClosed(event: dom.Event) extends WebSocketStreamException
  final case class WebSocketError[I](input: I) extends WebSocketStreamException

  sealed abstract class Driver[A] {

    def initialize(socket: dom.WebSocket): Unit

    def transmit(socket: dom.WebSocket, data: A): Unit
  }

  final object Driver {

    implicit val arrayBufferDriver: Driver[js.typedarray.ArrayBuffer] = binary(_ send _, "arraybuffer")
    implicit val blobDriver: Driver[dom.Blob]                         = binary(_ send _, "blob")
    implicit val stringDriver: Driver[String]                         = simple(_ send _)
    implicit val voidDriver: Driver[Void]                             = simple((_, _) => ())

    private def binary[A](send: (dom.WebSocket, A) => Unit, binaryType: String): Driver[A] =
      new Driver[A] {

        final def initialize(socket: dom.WebSocket): Unit = socket.binaryType = binaryType

        final def transmit(socket: dom.WebSocket, data: A): Unit = send(socket, data)
      }

    private def simple[A](send: (dom.WebSocket, A) => Unit): Driver[A] =
      new Driver[A] {

        final def initialize(socket: dom.WebSocket): Unit = ()

        final def transmit(socket: dom.WebSocket, data: A): Unit = send(socket, data)
      }
  }

  private sealed abstract class extract[O](project: dom.MessageEvent => Try[O]) {

    /**
      * Returns a stream that emits messages of type `O` from a [[dom.WebSocket websocket]] connection.
      *
      * Stream lifecycle:
      *  - A new websocket connection is established on start.
      *  - Connection termination, not initiated by this stream, is propagated as an error.
      *  - Incoming messages are propagated as events.
      *  - The connection is closed on stop.
      *
      * @param url absolute URL of websocket endpoint
      */
    def apply(url: String): EventStream[O] =
      apply[Void](url, EventStream.empty)

    /**
      * Returns a stream that emits messages of type `O` from a [[dom.WebSocket websocket]] connection.
      *
      * Transmission is supported for the following types:
      *  - [[js.typedarray.ArrayBuffer]]
      *  - [[dom.raw.Blob]]
      *  - [[String]]
      *
      * Stream lifecycle:
      *  - A new websocket connection is established on start.
      *  - Outgoing messages, if any, are sent on this connection.
      *    - Transmission failures, due to connection termination, are propagated as errors.
      *  - Connection termination, not initiated by this stream, is propagated as an error.
      *  - Incoming messages are propagated as events.
      *  - The connection is closed on stop.
      *
      * @param url      absolute URL of websocket endpoint
      * @param transmit messages to send to the websocket endpoint
      */
    def apply[I: Driver](url: String, transmit: EventStream[I]): EventStream[O] =
      new WebSocketEventStream(transmit, project, url)
  }

  private sealed abstract class data[O] extends extract(e => Try(e.data.asInstanceOf[String]))

  /** Builder for streams that emit [[dom.MessageEvent messages]] from a websocket connection */
  final case object raw extends extract(Success(_))

  /** Builder for streams that extract and emit text [[dom.MessageEvent#data data]] from a websocket connection */
  final case object text extends data[String]
}
