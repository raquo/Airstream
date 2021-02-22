package com.raquo.airstream.web

import com.raquo.airstream.core.Transaction
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.web.FetchEventStream.{ FetchError, FetchResponse, FetchTimeout }
import org.scalajs.dom
import org.scalajs.dom.experimental.Fetch.fetch
import org.scalajs.dom.experimental.{
  AbortController, ByteString, Headers, HttpMethod, ReferrerPolicy, RequestCache,
  RequestCredentials, RequestInit, RequestMode, RequestRedirect, Response, ResponseType
}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.scalajs.js.timers.{ clearTimeout, setTimeout, SetTimeoutHandle }
import scala.scalajs.js.{ |, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FetchEventStream[A](
  url: String,
  method: HttpMethod,
  headers: js.UndefOr[Map[String, String]],
  body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String],
  referrer: js.UndefOr[String],
  referrerPolicy: js.UndefOr[ReferrerPolicy],
  mode: js.UndefOr[RequestMode],
  credentials: js.UndefOr[RequestCredentials],
  cache: js.UndefOr[RequestCache],
  redirect: js.UndefOr[RequestRedirect],
  integrity: js.UndefOr[String],
  keepalive: js.UndefOr[Boolean],
  timeout: js.UndefOr[FiniteDuration],
  extract: Response => Promise[A]
) extends EventStream[FetchResponse[A]] {

  protected[airstream] val topoRank: Int = 1

  private val abortController = new AbortController()
  private var timeoutHandle: js.UndefOr[SetTimeoutHandle] = js.undefined

  override protected[this] def onStart(): Unit = {
    val response = sendRequest()

    timeout.foreach { timeout =>
      timeoutHandle = setTimeout(timeout) {
        abortController.abort()
        new Transaction(fireError(FetchTimeout(timeout), _))
      }
    }

    response.onComplete { result =>
      result.fold[Unit](
        handleError,
        response => {
          timeoutHandle.foreach(clearTimeout)
          timeoutHandle = js.undefined
          extract(response).toFuture.onComplete { extracted =>
            extracted.fold[Unit](
              handleError,
              extracted => {
                new Transaction(fireValue(
                  FetchResponse[A](
                    ok = response.ok,
                    status = response.status,
                    statusText = response.statusText,
                    headers = response.headers,
                    `type` = response.`type`,
                    data = extracted,
                    url = response.url
                  ), _
                ))
              }
            )
          }
        },
      )
    }
  }

  override protected[this] def onStop(): Unit = {
    abortController.abort()
  }

  private def handleError(error: Throwable): Unit = {
    timeoutHandle.foreach(clearTimeout)
    timeoutHandle = js.undefined
    new Transaction(fireError(FetchError(error), _))
  }

  private def sendRequest(): Future[Response] = {
    val init = js.Object().asInstanceOf[RequestInit]
    init.method = method
    init.headers = headers.map { headers =>
      val dict = js.Object().asInstanceOf[js.Dictionary[String]]
      headers.foreach { case (name, value) =>
        dict(name) = value
      }
      dict
    }
    init.body = body
    init.referrer = referrer
    init.referrerPolicy = referrerPolicy
    init.mode = mode
    init.credentials = credentials
    init.cache = cache
    init.redirect = redirect
    init.integrity = integrity
    init.keepalive = keepalive
    init.signal = abortController.signal
    fetch(url, init).toFuture
  }

}

object FetchEventStream {

  /**
    *
    * @param data Contains the extracted response data (string, json, blob, etc)
    * @param `type` Contains the type of the response.
    * @param url Contains the URL of the response.
    * @param ok Contains a boolean stating whether the response was successful (status in the range 200-299) or not.
    * @param status Contains the status code of the response (e.g., 200 for a success).
    * @param statusText Contains the status message corresponding to the status code (e.g., OK for 200).
    * @param headers Contains the Headers object associated with the response.
    * @tparam A type of the extracted data (String, js.Any for json, dom.Blob, etc)
    */
  final case class FetchResponse[A](
    ok: Boolean,
    status: Int,
    statusText: ByteString,
    headers: Headers,
    `type`: ResponseType,
    data: A,
    url: String,
  )

  sealed abstract class FetchStreamException extends Throwable

  final case class FetchError(cause: Any) extends FetchStreamException

  final case class FetchTimeout(timeout: FiniteDuration) extends FetchStreamException

}
