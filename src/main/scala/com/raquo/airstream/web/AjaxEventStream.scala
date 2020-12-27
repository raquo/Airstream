package com.raquo.airstream.web

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import org.scalajs.dom

/**
  * [[AjaxEventStream]] performs an HTTP request and emits the [[dom.XMLHttpRequest]]/[[dom.ext.AjaxException]] on
  * success/failure.
  *
  * The network request is delayed until start. In other words, the network request is not performed if the stream is
  * never started.
  *
  * On restart, a new request is performed and the subsequent success/failure is propagated downstream.
  */
class AjaxEventStream(
                       method: String,
                       url: String,
                       data: dom.ext.Ajax.InputData,
                       timeout: Int,
                       headers: Map[String, String],
                       withCredentials: Boolean,
                       responseType: String
                     ) extends EventStream[dom.XMLHttpRequest] {

  protected[airstream] val topoRank: Int = 1

  override protected[this] def onStart(): Unit = {
    // the implementation mirrors dom.ext.Ajax.apply
    val req = new dom.XMLHttpRequest
    req.onreadystatechange = (_: dom.Event) =>
      if (isStarted && req.readyState == 4) {
        val status = req.status
        if ((status >= 200 && status < 300) || status == 304)
          new Transaction(fireValue(req, _))
        else
          new Transaction(fireError(dom.ext.AjaxException(req), _))
      }
    req.open(method, url)
    req.responseType = responseType
    req.timeout = timeout.toDouble
    req.withCredentials = withCredentials
    headers.foreach(Function.tupled(req.setRequestHeader))
    if (data == null) req.send() else req.send(data)
  }
}

object AjaxEventStream {

  /**
    * Returns an [[EventStream]] that performs an HTTP request and emits the
    * [[dom.XMLHttpRequest]]/[[dom.ext.AjaxException]] on success/failure.
    *
    * The network request is delayed until start. In other words, the network request is not performed if the stream is
    * never started.
    *
    * On restart, a new request is performed and the subsequent success/failure is propagated downstream.
    *
    * @see [[dom.raw.XMLHttpRequest]] for a description of the parameters
    */
  def apply(
             method: String,
             url: String,
             data: dom.ext.Ajax.InputData,
             timeout: Int,
             headers: Map[String, String],
             withCredentials: Boolean,
             responseType: String
           ): EventStream[dom.XMLHttpRequest] =
    new AjaxEventStream(method, url, data, timeout, headers, withCredentials, responseType)

  /**
    * Returns an [[EventStream]] that performs an HTTP `DELETE` request.
    *
    * @see [[apply]]
    */
  def delete(
              url: String,
              data: dom.ext.Ajax.InputData = null,
              timeout: Int = 0,
              headers: Map[String, String] = Map.empty,
              withCredentials: Boolean = false,
              responseType: String = ""
            ): EventStream[dom.XMLHttpRequest] =
    apply("DELETE", url, data, timeout, headers, withCredentials, responseType)

  /**
    * Returns an [[EventStream]] that performs an HTTP `GET` request.
    *
    * @see [[apply]]
    */
  def get(
           url: String,
           data: dom.ext.Ajax.InputData = null,
           timeout: Int = 0,
           headers: Map[String, String] = Map.empty,
           withCredentials: Boolean = false,
           responseType: String = ""
         ): EventStream[dom.XMLHttpRequest] =
    apply("GET", url, data, timeout, headers, withCredentials, responseType)

  /**
    * Returns an [[EventStream]] that performs an HTTP `PATCH` request.
    *
    * @see [[apply]]
    */
  def patch(
             url: String,
             data: dom.ext.Ajax.InputData = null,
             timeout: Int = 0,
             headers: Map[String, String] = Map.empty,
             withCredentials: Boolean = false,
             responseType: String = ""
           ): EventStream[dom.XMLHttpRequest] =
    apply("PATCH", url, data, timeout, headers, withCredentials, responseType)

  /**
    * Returns an [[EventStream]] that performs an HTTP `POST` request.
    *
    * @see [[apply]]
    */
  def post(
            url: String,
            data: dom.ext.Ajax.InputData = null,
            timeout: Int = 0,
            headers: Map[String, String] = Map.empty,
            withCredentials: Boolean = false,
            responseType: String = ""
          ): EventStream[dom.XMLHttpRequest] =
    apply("POST", url, data, timeout, headers, withCredentials, responseType)

  /**
    * Returns an [[EventStream]] that performs an HTTP `PUT` request.
    *
    * @see [[apply]]
    */
  def put(
           url: String,
           data: dom.ext.Ajax.InputData = null,
           timeout: Int = 0,
           headers: Map[String, String] = Map.empty,
           withCredentials: Boolean = false,
           responseType: String = ""
         ): EventStream[dom.XMLHttpRequest] =
    apply("PUT", url, data, timeout, headers, withCredentials, responseType)
}