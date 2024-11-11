package com.raquo.airstream.web

import com.raquo.airstream.ownership.Owner
import org.scalajs.dom

import scala.util.{Success, Try}

/** This intermediate step is usually created via
  * [[WebStorageVar]] `object` methods.
  */
class WebStorageBuilder(
  maybeStorage: () => Option[dom.Storage],
  key: String,
  syncOwner: Option[Owner]
) {

  def text(default: => String): WebStorageVar[String] = {
    withCodec(
      encode = identity,
      decode = Success(_),
      default = Success(default),
      syncDistinctByFn = _ == _
    )
  }

  def bool(default: => Boolean): WebStorageVar[Boolean] = {
    withCodec[Boolean](
      encode = _.toString,
      decode = str => Try(str.toBoolean),
      default = Success(default),
      syncDistinctByFn = _ == _
    )
  }

  def int(default: => Int): WebStorageVar[Int] = {
    withCodec[Int](
      encode = _.toString,
      decode = str => Try(str.toInt),
      default = Success(default),
      syncDistinctByFn = _ == _
    )
  }

  /**
    * @param encode  Must not throw!
    * @param decode  Must not throw!
    * @param default If key is not found in storage either initially
    *                or at any future point, this value will be used instead.
    */
  def withCodec[A](
    encode: A => String,
    decode: String => Try[A],
    default: => Try[A],
    syncDistinctByFn: (A, A) => Boolean = (_: A) == (_: A),
  ): WebStorageVar[A] = {
    val storageVar = new WebStorageVar[A](
      maybeStorage, key, encode, decode, default,
      syncDistinctByFn
    )
    syncOwner.foreach(storageVar.syncFromExternalUpdates(_))
    storageVar
  }

}
