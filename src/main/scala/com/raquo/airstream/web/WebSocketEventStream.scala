package com.raquo.airstream.web

import com.raquo.airstream.core.{Observer, Transaction}
import com.raquo.airstream.eventstream.EventStream
import org.scalajs.dom

import scala.scalajs.js
import scala.util.{Success, Try}

/**
  * An event source that emits/transmits messages from/on a [[dom.WebSocket]] connection.
  *
  * '''Warning''': [[dom.WebSocket]] is an ugly, imperative JS construct. We set event callbacks for
  * `onclose`, `onmessage`, and if requested, also for `onerror`, `onopen`.
  * Make sure you don't override Airstream's listeners, or this stream will not work properly.
  *
  * @param url            absolute URL of websocket endpoint
  * @param protocol       name of the sub-protocol the server selected
  * @param closeObserver  called when a websocket connection is closed
  * @param errorObserver  called when a websocket connection error occurs
  * @param openObserver   called when a websocket connection is open
  * @param startObserver  called when a websocket connection is started
  * @param unsentObserver called when a message cannot be sent
  *
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API MDN]]
  */
class WebSocketEventStream[I, O] private (
  url: String,
  project: dom.MessageEvent => Try[I],
  closeObserver: Observer[dom.CloseEvent],
  errorObserver: Observer[dom.Event],
  openObserver: Observer[dom.Event],
  startObserver: Observer[dom.WebSocket],
  unsentObserver: Observer[O],
  protocol: String
)(implicit W: WebSocketEventStream.Writer[O])
  extends EventStream[I] {

  protected[airstream] val topoRank: Int = 1

  private var websocket: js.UndefOr[dom.WebSocket] = js.undefined

  def close(): Unit =
  // https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close
    websocket.foreach { socket =>
      socket.onclose = null
      socket.onerror = null
      socket.onmessage = null
      socket.onopen = null
      socket.close()
      websocket = js.undefined
    }

  def open(): Unit = {
    close()
    connect()
  }

  def send(out: O): Unit =
  // https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/send
  // The WebSocket.send() method enqueues the specified data to be transmitted to the server over the WebSocket
  // connection, increasing the value of bufferedAmount by the number of bytes needed to contain the data. If the
  // data can't be sent (for example, because it needs to be buffered but the buffer is full), the socket is closed
  // automatically.
    websocket.fold(unsentObserver.onNext(out))(W.write(_, out))

  override protected[this] def onStart(): Unit = {
    websocket.fold(connect())(bind)
    super.onStart()
  }

  override protected[this] def onStop(): Unit = {
    close()
    super.onStop()
  }

  private def bind(socket: dom.WebSocket): Unit =
  // bind message listener
    socket.onmessage = (e: dom.MessageEvent) => {
      val _ = project(e).fold(e => new Transaction(fireError(e, _)), o => new Transaction(fireValue(o, _)))
    }

  private def connect(): Unit = {
    val socket = new dom.WebSocket(url, protocol)

    // update local reference
    websocket = socket

    // initialize new socket
    W.initialize(socket)

    // bind message listener
    if (isStarted) bind(socket)

    // register required listeners
    socket.onclose = (e: dom.CloseEvent) => {
      websocket = js.undefined
      if (closeObserver ne Observer.empty) closeObserver.onNext(e)
    }

    // register optional listeners
    if (errorObserver ne Observer.empty) socket.onerror = errorObserver.onNext
    if (openObserver ne Observer.empty) socket.onopen = openObserver.onNext

    // call optional observer
    if (startObserver ne Observer.empty) startObserver.onNext(socket)
  }
}

object WebSocketEventStream {

  /**
    * A unidirectional (server to client) websocket connection defined as 2 values,
    *  1. controller: observer that controls when a connection is opened/closed
    *  1. receiver: stream of incoming messages of type `I`
    */
  type Simplex[+I] = (Observer[Boolean], EventStream[I])

  /**
    * A bidirectional websocket connection defined as 3 values,
    *  1. controller: observer that controls when a connection is opened/closed
    *  1. receiver: stream of incoming messages of type `I`
    *  1. transmitter: observer that transmits outgoing messages of type `O`
    */
  type Duplex[+I, -O] = (Observer[Boolean], EventStream[I], Observer[O])

  sealed abstract class Reader[I](project: dom.MessageEvent => Try[I]) {

    /**
      * Returns a websocket [[Duplex connection]] as 3 values,
      *  1. `controller`: observer that controls when a connection is opened/closed
      *  1. `receiver`: stream of incoming messages of type `I`
      *  1. `transmitter`: observer that transmits outgoing messages of type `O`
      *
      * Connection lifecycle:
      *  - A new websocket connection is established when either
      *    - the `receiver` is started.
      *    - the `controller` is called with a value of true.
      *  - A connection is closed when either
      *    - the `receiver` is stopped.
      *    - the `controller` is called with a value of false.
      *  - The `closeObserver` is called when the connection is terminated abruptly.
      *    - This can be used in conjunction with the `controller` to implement a retry policy.
      *
      * Transmission:
      *  - The `transmitter` sends outgoing messages on the underlying websocket connection, if any.
      *    - If no connection exists, the message is passed on to the `unsentObserver`.
      *    - Note that a connection is automatically closed when
      *    [[https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/send data can't be sent]].
      *
      * @param url absolute URL of websocket endpoint
      * @param closeObserver called when a websocket connection is closed
      * @param errorObserver called when a websocket connection error occurs
      * @param openObserver called when a websocket connection is opened
      * @param startObserver called when a websocket connection is started
      * @param unsentObserver called when transmission is attempted on a non existing connection
      * @param protocol websocket server sub-protocol
      */
    final def apply[O: Writer](
      url: String,
      closeObserver: Observer[dom.CloseEvent] = Observer.empty,
      errorObserver: Observer[dom.Event] = Observer.empty,
      openObserver: Observer[dom.Event] = Observer.empty,
      startObserver: Observer[dom.WebSocket] = Observer.empty,
      unsentObserver: Observer[O] = Observer.empty,
      protocol: String = ""
    ): Duplex[I, O] = {
      val ws = new WebSocketEventStream(
        url,
        project,
        closeObserver,
        errorObserver,
        openObserver,
        startObserver,
        unsentObserver,
        protocol
      )
      (Observer(if (_) ws.open() else ws.close()), ws, Observer(ws.send))
    }

    /**
      * Returns a websocket [[Simplex connection]] as 2 values,
      *  1. `controller`: observer that controls when a connection is opened/closed
      *  1. `receiver`: stream of incoming messages of type `I`
      *
      * Connection lifecycle:
      *  - A new websocket connection is established when either
      *    - the `receiver` is started.
      *    - the `controller` is called with a value of true.
      *  - A connection is closed when either
      *    - the `receiver` is stopped.
      *    - the `controller` is called with a value of false.
      *  - The `closeObserver` is called when the connection is terminated abruptly.
      *    - This can be used in conjunction with the `controller` to implement a retry policy.
      *
      * @param url absolute URL of websocket endpoint
      * @param closeObserver called when a websocket connection is closed
      * @param errorObserver called when a websocket connection error occurs
      * @param openObserver called when a websocket connection is opened
      * @param startObserver called when a websocket connection is started
      * @param protocol websocket server sub-protocol
      * @return
      */
    final def read(
      url: String,
      closeObserver: Observer[dom.CloseEvent] = Observer.empty,
      errorObserver: Observer[dom.Event] = Observer.empty,
      openObserver: Observer[dom.Event] = Observer.empty,
      startObserver: Observer[dom.WebSocket] = Observer.empty,
      protocol: String = ""
    ): Simplex[I] = {
      val (control, receiver, _) =
        apply[Void](url, closeObserver, errorObserver, openObserver, startObserver, Observer.empty, protocol)
      (control, receiver)
    }
  }

  sealed abstract class Driver[A: Writer] extends Reader(e => Try(e.data.asInstanceOf[A])) {

    /** @see [[apply]] */
    final def open(
      url: String,
      closeObserver: Observer[dom.CloseEvent] = Observer.empty,
      errorObserver: Observer[dom.Event] = Observer.empty,
      openObserver: Observer[dom.Event] = Observer.empty,
      startObserver: Observer[dom.WebSocket] = Observer.empty,
      unsentObserver: Observer[A] = Observer.empty,
      protocol: String = ""
    ): Duplex[A, A] =
      apply(url, closeObserver, errorObserver, openObserver, startObserver, unsentObserver, protocol)
  }

  sealed abstract class Writer[O] {
    def initialize(socket: dom.WebSocket): Unit
    def write(socket: dom.WebSocket, data: O): Unit
  }

  object Writer {

    implicit val arraybuffer: Writer[js.typedarray.ArrayBuffer] = binary(_ send _, "arraybuffer")
    implicit val blob: Writer[dom.Blob]                         = binary(_ send _, "blob")
    implicit val string: Writer[String]                         = simple(_ send _)
    implicit val noop: Writer[Void]                             = simple((_, _) => ())

    private def binary[A](f: (dom.WebSocket, A) => Unit, binaryType: String): Writer[A] =
      new Writer[A] {
        final def initialize(socket: dom.WebSocket): Unit     = socket.binaryType = binaryType
        final def write(socket: dom.WebSocket, data: A): Unit = f(socket, data)
      }

    private def simple[A](f: (dom.WebSocket, A) => Unit): Writer[A] =
      new Writer[A] {
        final def initialize(socket: dom.WebSocket): Unit     = ()
        final def write(socket: dom.WebSocket, data: A): Unit = f(socket, data)
      }
  }

  object arraybuffer extends Driver[js.typedarray.ArrayBuffer]
  object blob        extends Driver[dom.Blob]
  object raw         extends Reader(Success(_))
  object text        extends Driver[String]
}
