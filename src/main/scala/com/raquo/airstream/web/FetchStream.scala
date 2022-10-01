package com.raquo.airstream.web

import com.raquo.airstream.core.{EventStream, InternalObserver, Transaction, WritableStream}
import org.scalajs.dom

import scala.scalajs.js

/** Make requests using the Fetch API, the modern alternative to Ajax.
  *
  * @see https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API
  *
  * Use get / post / apply / etc. methods on FetchStream companion object to make Fetch requests.
  */
class FetchStream private[web] (
  url: String,
  requestInit: dom.RequestInit,
  maybeAbortController: js.UndefOr[dom.AbortController],
  maybeAbortStream: js.UndefOr[EventStream[Any]],
  shouldAbortOnStop: Boolean
) extends WritableStream[js.Promise[dom.Response]] {

  // #TODO How do we go about caching the response? We have a task about adding emitOnce to AjaxStream... something similar here?
  //  - Perhaps we can decide this after finishing the semantic behaviour change?

  // TODO[API] Not sure if FetchStream re-emitting abortStream errors is desired

  // #Note:
  //  - if maybeAbortController is defined, either maybeAbortStream or shouldAbortOnStop or both will be defined/true.
  //  - if maybeAbortController is empty, both maybeAbortStream and shouldAbortOnStop are empty/false

  override protected val topoRank: Int = 1

  private val maybeAbortStreamObserver: js.UndefOr[InternalObserver[Any]] = {
    maybeAbortStream.map { _ =>
      InternalObserver[Any](
        onNext = (_, _) => maybeAbortController.get.abort(),
        onError = (err, _) => new Transaction(fireError(err, _))
      )
    }
  }

  override protected def onWillStart(): Unit = {
    maybeAbortStream.foreach { abortStream =>
      abortStream.addInternalObserver(maybeAbortStreamObserver.get, shouldCallMaybeWillStart = true)
    }
    val responsePromise = dom.Fetch.fetch(url, requestInit)
    new Transaction(fireValue(responsePromise, _))
  }

  override protected def onStop(): Unit = {
    if (shouldAbortOnStop) {
      maybeAbortController.get.abort()
    }
  }

}

/** Note: dom.BodyInit is a union type that includes String
  * and some other Javascript-specific data types.
  */
object FetchStream extends FetchBuilder[dom.BodyInit, String](
  encodeRequest = identity,
  decodeResponse = response => EventStream.fromJsPromise(response.text())
) {

  lazy val raw: FetchBuilder[dom.BodyInit, dom.Response] = {
    new FetchBuilder(identity, EventStream.fromValue(_))
  }

  def withCodec[In, Out](
    encodeRequest: In => dom.BodyInit,
    decodeResponse: dom.Response => EventStream[Out]
  ): FetchBuilder[In, Out] = {
    new FetchBuilder(encodeRequest, decodeResponse)
  }

  def withEncoder[In](
    encodeRequest: In => dom.BodyInit,
  ): FetchBuilder[In, String] = {
    new FetchBuilder(encodeRequest, response => EventStream.fromJsPromise(response.text()))
  }

  def withDecoder[Out](
    decodeResponse: dom.Response => EventStream[Out]
  ): FetchBuilder[dom.BodyInit, Out] = {
    new FetchBuilder(encodeRequest = identity, decodeResponse)
  }

}

class FetchBuilder[In, Out](
  encodeRequest: In => dom.BodyInit,
  decodeResponse: dom.Response => EventStream[Out]
) {

  def get(
    url: String,
    setOptions: (FetchOptions[In] => Unit)*
  ): EventStream[Out] = {
    apply(_.GET, url, setOptions: _*)
  }

  def post(
    url: String,
    setOptions: (FetchOptions[In] => Unit)*
  ): EventStream[Out] = {
    apply(_.POST, url, setOptions: _*)
  }

  def put(
    url: String,
    setOptions: (FetchOptions[In] => Unit)*
  ): EventStream[Out] = {
    apply(_.PUT, url, setOptions: _*)
  }

  def apply(
    method: dom.HttpMethod.type => dom.HttpMethod,
    url: String,
    setOptions: (FetchOptions[In] => Unit)*
  ): EventStream[Out] = {
    val (request, maybeAbortController, maybeAbortStream, shouldAbortOnStop) = {
      val options = new FetchOptions[In](encodeRequest)
      setOptions.foreach(setOption => setOption(options))
      options.request.method = method(dom.HttpMethod)
      (options.request, options.maybeAbortController, options.maybeAbortStream, options.shouldAbortOnStop)
    }

    val fetchStream = new FetchStream(url, request, maybeAbortController, maybeAbortStream, shouldAbortOnStop)

    fetchStream.flatMap(EventStream.fromJsPromise(_)).flatMap(decodeResponse)
  }
}

class FetchOptions[In] private[web] (
  encodeRequest: In => dom.BodyInit
) {

  private[web] val request: dom.RequestInit = new dom.RequestInit {}

  private var maybeHeaders: js.UndefOr[dom.Headers] = js.undefined

  private[web] var maybeAbortController: js.UndefOr[dom.AbortController] = js.undefined

  private[web] var maybeAbortStream: js.UndefOr[EventStream[Any]] = js.undefined

  private[web] var shouldAbortOnStop: Boolean = false

  private def getOrCreateAbortController(): dom.AbortController = {
    maybeAbortController.getOrElse {
      val controller = new dom.AbortController
      maybeAbortController = controller
      request.signal = controller.signal
      controller
    }
  }

  /** Set headers, overriding previous values for the corresponding keys.
    * @param kvs (key1 -> value1, key2 -> value2)
    */
  def headers(kvs: (String, String)*): Unit = {
    if (maybeHeaders.isEmpty) {
      val headers = new dom.Headers()
      maybeHeaders = headers
      request.headers = headers
    }
    kvs.foreach { kv =>
      maybeHeaders.get.set(name = kv._1, value = kv._2)
    }
  }

  /** Append headers – this is like setting headers, but for those keys that
    * accept multiple values, the provided value(s) will be added to the key
    * without removing the previously set value.
    *
    * @param kvs (key1 -> value1, key2 -> value2)
    */
  def headersAppend(kvs: (String, String)*): Unit = {
    if (maybeHeaders.isEmpty) {
      val headers = new dom.Headers()
      maybeHeaders = headers
      request.headers = headers
    }
    kvs.foreach { kv =>
      maybeHeaders.get.append(name = kv._1, value = kv._2)
    }
  }

  /** Abort the Fetch request when `abortStream` emits.
    * This is a wrapper for https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal
    * Errors emitted by abortStream will be re-emitted by FetchStream.
    */
  def abortStream(source: EventStream[Any]): Unit = {
    getOrCreateAbortController()
    maybeAbortStream = source
  }

  /** Abort the fetch request if FetchStream is stopped. (False by default) */
  def abortOnStop(): Unit = {
    getOrCreateAbortController()
    shouldAbortOnStop = true
  }

  def body(content: In): Unit = {
    request.body = encodeRequest(content)
  }

  def mode(get: dom.RequestMode.type => dom.RequestMode): Unit = {
    request.mode = get(dom.RequestMode)
  }

  def credentials(get: dom.RequestCredentials.type => dom.RequestCredentials): Unit = {
    request.credentials = get(dom.RequestCredentials)
  }

  def cache(get: dom.RequestCache.type => dom.RequestCache): Unit = {
    request.cache = get(dom.RequestCache)
  }

  def redirect(get: dom.RequestRedirect.type => dom.RequestRedirect): Unit = {
    request.redirect = get(dom.RequestRedirect)
  }

  def referrer(url: String): Unit = {
    request.referrer = url
  }

  def referrerClear(): Unit = {
    request.referrer = ""
  }

  def referrerPolicy(get: dom.ReferrerPolicy.type => dom.ReferrerPolicy): Unit = {
    request.referrerPolicy = get(dom.ReferrerPolicy)
  }

  def integrity(hash: String): Unit = {
    request.integrity = hash
  }

  def keepAlive(value: Boolean): Unit = {
    request.keepalive = value
  }

  def empty(): Unit = {
    // Do nothing. Just a helper for easier composition
  }

}
