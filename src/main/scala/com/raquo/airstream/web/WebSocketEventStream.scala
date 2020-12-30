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
class WebSocketEventStream[I, O](
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

  /**
    * Builder for unidirectional websocket stream.
    *
    * @param url absolute URL of a websocket endpoint,
    *            use [[websocketUrl]] to construct an absolute URL from a relative one
    */
  def apply(url: String): Builder[Void] =
    new Builder[Void](EventStream.empty, url)

  /**
    * Builder for bidirectional websocket stream.
    *
    * Transmission is supported for the following types:
    *  - [[js.typedarray.ArrayBuffer]]
    *  - [[dom.raw.Blob]]
    *  - [[String]]
    *
    * @param url absolute URL of a websocket endpoint,
    *            use [[websocketUrl]] to construct an absolute URL from a relative one
    * @param transmit message to be transmitted from client to server
    */
  def apply[I: Driver](url: String, transmit: EventStream[I]): Builder[I] =
    new Builder(transmit, url)

  private def apply[I: Driver, O](
    transmit: EventStream[I],
    project: dom.MessageEvent => Try[O],
    url: String
  ): EventStream[O] =
    new WebSocketEventStream(transmit, project, url)

  sealed abstract class WebSocketStreamException extends Exception

  final case class WebSocketClosed(event: dom.Event) extends WebSocketStreamException
  final case class WebSocketError[I](input: I) extends WebSocketStreamException

  sealed abstract class Driver[A] {

    def initialize(socket: dom.WebSocket): Unit

    def transmit(socket: dom.WebSocket, data: A): Unit
  }

  final class Builder[I: Driver](transmit: EventStream[I], url: String) {

    /**
      * Returns a stream that extracts data from raw [[dom.MessageEvent messages]] and emits them.
      *
      * @see [[raw]]
      */
    def data[O]: EventStream[O] =
      WebSocketEventStream(transmit, m => Try(m.data.asInstanceOf[O]), url)

    /**
      * Returns a stream that emits [[dom.MessageEvent messages]] from a [[dom.WebSocket websocket]] connection.
      *
      * Stream lifecycle:
      *  - A new websocket connection is established on start.
      *  - Outgoing messages, if any, are sent on this connection.
      *    - Transmission failures, due to connection termination, are propagated as errors.
      *  - Connection termination, not initiated by this stream, is propagated as an error.
      *  - Incoming messages are propagated as events.
      *  - The connection is closed on stop.
      */
    def raw: EventStream[dom.MessageEvent] =
      WebSocketEventStream(transmit, Success.apply, url)

    /**
      * Returns a stream that extracts text data from raw [[dom.MessageEvent messages]] and emits them.
      *
      * @see [[raw]]
      */
    def text: EventStream[String] =
      data[String]
  }

  object Driver {

    implicit val binaryDriver: Driver[js.typedarray.ArrayBuffer] = binary(_ send _, "arraybuffer")
    implicit val blobDriver: Driver[dom.Blob] = binary(_ send _, "blob")
    implicit val stringDriver: Driver[String] = simple(_ send _)
    implicit val voidDriver: Driver[Void] = simple((_, _) => ())

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
}
