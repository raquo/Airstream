package com.raquo.airstream.web

import com.raquo.airstream.core.{Observer, Transaction}
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.web.AjaxEventStream.{AjaxAbort, AjaxError, AjaxStreamException, AjaxTimeout}
import org.scalajs.dom

import scala.scalajs.js

// @TODO[Test] Needs testing

/**
  * [[AjaxEventStream]] performs an HTTP request and emits an [[dom.XMLHttpRequest]] on success,
  * or an [[AjaxStreamException]] error (AjaxError | AjaxTimeout | AjaxAbort) on failure.
  *
  * Acceptable HTTP response status codes are 2xx and 304, others result in AjaxError.
  *
  * The network request is only performed when the stream is started.
  *
  * When stream is restarted, a new request is performed, and the subsequent response is emitted.
  * The previous request is not aborted, but its response will be ignored.
  *
  * Warning: dom.XmlHttpRequest is an ugly, imperative JS construct. We set event callbacks for
  * onload, onerror, onabort, ontimeout, and if requested, also for onprogress and onreadystatechange.
  * Make sure you don't override Airstream's listeners, or this stream will not work properly.
  *
  * @see [[dom.raw.XMLHttpRequest]] for a description of the parameters
  *
  * @param progressObserver         - optional, pass Observer.empty if not needed.
  * @param readyStateChangeObserver - optional, pass Observer.empty if not needed.
  */
class AjaxEventStream(
  method: String,
  url: String,
  data: dom.ext.Ajax.InputData,
  timeout: Int,
  headers: Map[String, String],
  withCredentials: Boolean,
  responseType: String,
  progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
  readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
) extends EventStream[dom.XMLHttpRequest] {

  protected[airstream] val topoRank: Int = 1

  private var pendingRequest: Option[dom.XMLHttpRequest] = None

  override protected[this] def onStart(): Unit = {
    val request = AjaxEventStream.openRequest(method, url, timeout, headers, withCredentials, responseType)

    pendingRequest = Some(request)

    // As far as I can tell, only one of onload / onerror / onabort / ontimeout events can fire for every request,
    // so the callbacks below are both mutually exhaustive and encompass all possible outcomes.
    // If this is not the case, we have a problem.

    // Note: XMLHttpRequest is a valid target for native JS addEventListener, which is a better API,
    // but that pattern isn't supported on XMLHttpRequest by all browsers (e.g. IE11 doesn't work).

    request.onload = (_: dom.Event) => {
      if (pendingRequest.contains(request)) {
        pendingRequest = None
        val status = request.status
        if ((status >= 200 && status < 300) || status == 304)
          new Transaction(fireValue(request, _))
        else
          new Transaction(fireError(AjaxError(request, s"Bad HTTP response status code: $status"), _))
      }
    }

    request.onerror = (ev: dom.ErrorEvent) => {
      if (pendingRequest.contains(request)) {
        pendingRequest = None
        new Transaction(fireError(AjaxError(request, ev.message), _))
      }
    }

    request.onabort = (_: js.Any) => {
      if (pendingRequest.contains(request)) {
        pendingRequest = None
        new Transaction(fireError(AjaxAbort(request), _))
      }
    }

    request.ontimeout = (_: dom.Event) => {
      if (pendingRequest.contains(request)) {
        pendingRequest = None
        new Transaction(fireError(AjaxTimeout(request), _))
      }
    }

    // The following observers are optional.

    if (progressObserver != Observer.empty) {
      request.onprogress = ev => {
        if (pendingRequest.contains(request)) {
          progressObserver.onNext((request, ev))
        }
      }
    }

    if (readyStateChangeObserver != Observer.empty) {
      request.onreadystatechange = (_: dom.Event) => {
        if (pendingRequest.contains(request)) {
          readyStateChangeObserver.onNext(request)
        }
      }
    }

    AjaxEventStream.sendRequest(request, data)
  }

  /** This stream will emit at most one event per request regardless of the outcome.
    *
    * You need to introspect the result to determine whether the request
    * succeeded, failed, timed out, or was aborted.
    */
  lazy val completeEvents: EventStream[dom.XMLHttpRequest] = {
    this.recover {
      case err: AjaxStreamException => Some(err.xhr)
    }
  }

  override protected[this] def onStop(): Unit = {
    pendingRequest = None
  }
}

object AjaxEventStream {

  /** A more detailed version of [[dom.ext.AjaxException]] (no relation) */
  sealed abstract class AjaxStreamException(val xhr: dom.XMLHttpRequest) extends Exception

  final case class AjaxError(override val xhr: dom.XMLHttpRequest, message: String) extends AjaxStreamException(xhr)

  final case class AjaxTimeout(override val xhr: dom.XMLHttpRequest) extends AjaxStreamException(xhr)

  final case class AjaxAbort(override val xhr: dom.XMLHttpRequest) extends AjaxStreamException(xhr)

  /**
    * Returns an [[EventStream]] that performs an HTTP `GET` request.
    *
    * @see [[AjaxEventStream]]
    */
  def get(
    url: String,
    data: dom.ext.Ajax.InputData = null,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = "",
    progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
    readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
  ): AjaxEventStream = {
    new AjaxEventStream("GET", url, data, timeout, headers, withCredentials, responseType, progressObserver, readyStateChangeObserver)
  }

  /**
    * Returns an [[EventStream]] that performs an HTTP `POST` request.
    *
    * @see [[AjaxEventStream]]
    */
  def post(
    url: String,
    data: dom.ext.Ajax.InputData = null,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = "",
    progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
    readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
  ): AjaxEventStream = {
    new AjaxEventStream("POST", url, data, timeout, headers, withCredentials, responseType, progressObserver, readyStateChangeObserver)
  }

  /**
    * Returns an [[EventStream]] that performs an HTTP `PUT` request.
    *
    * @see [[AjaxEventStream]]
    */
  def put(
    url: String,
    data: dom.ext.Ajax.InputData = null,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = "",
    progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
    readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
  ): AjaxEventStream = {
    new AjaxEventStream("PUT", url, data, timeout, headers, withCredentials, responseType, progressObserver, readyStateChangeObserver)
  }

  /**
    * Returns an [[EventStream]] that performs an HTTP `PATCH` request.
    *
    * @see [[AjaxEventStream]]
    */
  def patch(
    url: String,
    data: dom.ext.Ajax.InputData = null,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = "",
    progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
    readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
  ): AjaxEventStream = {
    new AjaxEventStream("PATCH", url, data, timeout, headers, withCredentials, responseType, progressObserver, readyStateChangeObserver)
  }

  /**
    * Returns an [[EventStream]] that performs an HTTP `DELETE` request.
    *
    * @see [[AjaxEventStream]]
    */
  def delete(
    url: String,
    data: dom.ext.Ajax.InputData = null,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = "",
    progressObserver: Observer[(dom.XMLHttpRequest, dom.ProgressEvent)] = Observer.empty,
    readyStateChangeObserver: Observer[dom.XMLHttpRequest] = Observer.empty
  ): AjaxEventStream = {
    new AjaxEventStream("DELETE", url, data, timeout, headers, withCredentials, responseType, progressObserver, readyStateChangeObserver)
  }

  /** Initializes and configures the XmlHttpRequest. This does not cause any network activity.
    *
    * Note: `data` is added later, when actually sending the request.
    *
    * AjaxEventStream already does this internally. This is provided as a building block for custom logic.
    */
  def openRequest(
    method: String,
    url: String,
    timeout: Int = 0,
    headers: Map[String, String] = Map.empty,
    withCredentials: Boolean = false,
    responseType: String = ""
  ): dom.XMLHttpRequest = {
    val request = new dom.XMLHttpRequest
    request.open(method, url)
    request.responseType = responseType
    request.timeout = timeout.toDouble
    request.withCredentials = withCredentials
    headers.foreach(Function.tupled(request.setRequestHeader))
    request
  }

  /** Initiates network request. The request should be configured with all the callbacks by this point.
    *
    * AjaxEventStream already does this internally. This is provided as a building block for custom logic.
    */
  def sendRequest(
    request: dom.XMLHttpRequest,
    data: dom.ext.Ajax.InputData = null
  ): Unit = {
    if (data == null) request.send() else request.send(data)
  }
}
