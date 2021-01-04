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
      _url = url,
      _method = method,
      _headers = headers,
      _body = body
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
  private var _url: String,
  private var _method: HttpMethod,
  private var _headers: js.UndefOr[Map[String, String]] = js.undefined,
  private var _body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String] = js.undefined,
  private var _referrer: js.UndefOr[String] = js.undefined,
  private var _referrerPolicy: js.UndefOr[ReferrerPolicy] = js.undefined,
  private var _mode: js.UndefOr[RequestMode] = js.undefined,
  private var _credentials: js.UndefOr[RequestCredentials] = js.undefined,
  private var _cache: js.UndefOr[RequestCache] = js.undefined,
  private var _redirect: js.UndefOr[RequestRedirect] = js.undefined,
  private var _integrity: js.UndefOr[String] = js.undefined,
  private var _keepalive: js.UndefOr[Boolean] = js.undefined,
  private var _timeout: js.UndefOr[FiniteDuration] = js.undefined
) {

  def raw: FetchEventStream[Response] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = response => Promise.resolve[Response](response)
    )

  def readableStream: FetchEventStream[ReadableStream[Uint8Array]] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = response => Promise.resolve[ReadableStream[Uint8Array]](response.body)
    )

  def text: FetchEventStream[String] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = _.text()
    )

  def json: FetchEventStream[js.Any] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = _.json()
    )

  def blob: FetchEventStream[Blob] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = _.blob()
    )

  def arrayBuffer: FetchEventStream[ArrayBuffer] =
    new FetchEventStream(
      url = _url,
      method = _method,
      headers = _headers,
      body = _body,
      referrer = _referrer,
      referrerPolicy = _referrerPolicy,
      mode = _mode,
      credentials = _credentials,
      cache = _cache,
      redirect = _redirect,
      integrity = _integrity,
      keepalive = _keepalive,
      timeout = _timeout,
      extract = _.arrayBuffer()
    )

  def body(
    body: js.UndefOr[dom.Blob | dom.crypto.BufferSource | dom.FormData | String]
  ): FetchEventStreamBuilder = {
    this._body = body
    this
  }

  def referrer(
    referrer: js.UndefOr[String]
  ): FetchEventStreamBuilder = {
    this._referrer = referrer
    this
  }

  def referrerPolicy(
    referrerPolicy: js.UndefOr[ReferrerPolicy]
  ): FetchEventStreamBuilder = {
    this._referrerPolicy = referrerPolicy
    this
  }

  def mode(
    mode: js.UndefOr[RequestMode]
  ): FetchEventStreamBuilder = {
    this._mode = mode
    this
  }

  def credentials(
    credentials: js.UndefOr[RequestCredentials]
  ): FetchEventStreamBuilder = {
    this._credentials = credentials
    this
  }

  def cache(
    cache: js.UndefOr[RequestCache]
  ): FetchEventStreamBuilder = {
    this._cache = cache
    this
  }


  def redirect(
    redirect: js.UndefOr[RequestRedirect]
  ): FetchEventStreamBuilder = {
    this._redirect = redirect
    this
  }


  def integrity(
    integrity: js.UndefOr[String]
  ): FetchEventStreamBuilder = {
    this._integrity = integrity
    this
  }


  def keepalive(
    keepalive: js.UndefOr[Boolean]
  ): FetchEventStreamBuilder = {
    this._keepalive = keepalive
    this
  }


  def timeout(
    timeout: js.UndefOr[FiniteDuration]
  ): FetchEventStreamBuilder = {
    this._timeout = timeout
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
    this._url = url
    this._method = method
    this._headers = headers
    this._body = body
    this._referrer = referrer
    this._referrerPolicy = referrerPolicy
    this._mode = mode
    this._credentials = credentials
    this._cache = cache
    this._redirect = redirect
    this._integrity = integrity
    this._keepalive = keepalive
    this._timeout = timeout
    this
  }

}

