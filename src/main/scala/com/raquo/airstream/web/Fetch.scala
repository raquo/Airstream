package com.raquo.airstream.web

import org.scalajs.dom
import org.scalajs.dom.experimental.{ HttpMethod, ReadableStream, ReferrerPolicy, RequestCache, RequestCredentials, RequestMode, RequestRedirect, Response }
import org.scalajs.dom.Blob

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.scalajs.js.{ |, Promise }
import scala.scalajs.js.typedarray.{ ArrayBuffer, Uint8Array }

object Fetch {

  @inline def apply(
    method: HttpMethod,
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
  ): FetchEventStreamBuilder =
    new FetchEventStreamBuilder(
      url = url,
      method = method,
      headers = headers,
      body = body
    )

  @inline def get(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.GET,
      headers = headers,
    )

  @inline def post(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.POST,
      headers = headers,
      body = body
    )

  @inline def put(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.PUT,
      headers = headers,
      body = body
    )

  @inline def patch(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.PATCH,
      headers = headers,
      body = body
    )

  @inline def delete(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.DELETE,
      headers = headers,
    )

  @inline def query(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.QUERY,
      headers = headers,
    )

  @inline def head(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.HEAD,
      headers = headers,
    )

  @inline def options(
    url: String,
    headers: js.UndefOr[Map[String, String]] = js.undefined,
  ): FetchEventStreamBuilder =
    apply(
      url = url,
      method = HttpMethod.OPTIONS,
      headers = headers,
    )

}


class FetchEventStreamBuilder(
  private var url: String,
  private var method: HttpMethod,
  private var headers: js.UndefOr[Map[String, String]] = js.undefined,
  private var body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
  private var referrer: js.UndefOr[String] = js.undefined,
  private var referrerPolicy: js.UndefOr[ReferrerPolicy] = js.undefined,
  private var mode: js.UndefOr[RequestMode] = js.undefined,
  private var credentials: js.UndefOr[RequestCredentials] = js.undefined,
  private var cache: js.UndefOr[RequestCache] = js.undefined,
  private var redirect: js.UndefOr[RequestRedirect] = js.undefined,
  private var integrity: js.UndefOr[String] = js.undefined,
  private var keepalive: js.UndefOr[Boolean] = js.undefined,
  private var timeout: js.UndefOr[FiniteDuration] = js.undefined
) {

  def raw: FetchEventStream[Response] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = response => Promise.resolve[Response](response)
    )

  def readableStream: FetchEventStream[ReadableStream[Uint8Array]] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = response => Promise.resolve[ReadableStream[Uint8Array]](response.body)
    )

  def text: FetchEventStream[String] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = _.text()
    )

  def json: FetchEventStream[js.Any] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = _.json()
    )

  def blob: FetchEventStream[Blob] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = _.blob()
    )

  def arrayBuffer: FetchEventStream[ArrayBuffer] =
    new FetchEventStream(
      url = url,
      method = method,
      headers = headers,
      body = body,
      referrer = referrer,
      referrerPolicy = referrerPolicy,
      mode = mode,
      credentials = credentials,
      cache = cache,
      redirect = redirect,
      integrity = integrity,
      keepalive = keepalive,
      timeout = timeout,
      extract = _.arrayBuffer()
    )

  def body(
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String]
  ): FetchEventStreamBuilder = {
    this.body = body
    this
  }

  def referrer(
    referrer: js.UndefOr[String]
  ): FetchEventStreamBuilder = {
    this.referrer = referrer
    this
  }

  def referrerPolicy(
    referrerPolicy: js.UndefOr[ReferrerPolicy]
  ): FetchEventStreamBuilder = {
    this.referrerPolicy = referrerPolicy
    this
  }

  def mode(
    mode: js.UndefOr[RequestMode]
  ): FetchEventStreamBuilder = {
    this.mode = mode
    this
  }

  def credentials(
    credentials: js.UndefOr[RequestCredentials]
  ): FetchEventStreamBuilder = {
    this.credentials = credentials
    this
  }

  def cache(
    cache: js.UndefOr[RequestCache]
  ): FetchEventStreamBuilder = {
    this.cache = cache
    this
  }


  def redirect(
    redirect: js.UndefOr[RequestRedirect]
  ): FetchEventStreamBuilder = {
    this.redirect = redirect
    this
  }


  def integrity(
    integrity: js.UndefOr[String]
  ): FetchEventStreamBuilder = {
    this.integrity = integrity
    this
  }


  def keepalive(
    keepalive: js.UndefOr[Boolean]
  ): FetchEventStreamBuilder = {
    this.keepalive = keepalive
    this
  }


  def timeout(
    timeout: js.UndefOr[FiniteDuration]
  ): FetchEventStreamBuilder = {
    this.timeout = timeout
    this
  }

  def configure(
   url: String,
   method: HttpMethod,
   headers: js.UndefOr[Map[String, String]] = js.undefined,
   body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
   referrer: js.UndefOr[String] = js.undefined,
   referrerPolicy: js.UndefOr[ReferrerPolicy] = js.undefined,
   mode: js.UndefOr[RequestMode] = js.undefined,
   credentials: js.UndefOr[RequestCredentials] = js.undefined,
   cache: js.UndefOr[RequestCache] = js.undefined,
   redirect: js.UndefOr[RequestRedirect] = js.undefined,
   integrity: js.UndefOr[String] = js.undefined,
   keepalive: js.UndefOr[Boolean] = js.undefined,
   timeout: js.UndefOr[FiniteDuration] = js.undefined
  ): FetchEventStreamBuilder = {
    this.url = url
    this.method = method
    this.headers = headers
    this.body = body
    this.referrer = referrer
    this.referrerPolicy = referrerPolicy
    this.mode = mode
    this.credentials = credentials
    this.cache = cache
    this.redirect = redirect
    this.integrity = integrity
    this.keepalive = keepalive
    this.timeout = timeout
    this
  }

}

