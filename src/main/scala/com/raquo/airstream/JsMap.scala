package com.raquo.airstream

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSName}

// @TODO This is a temporary solution for https://github.com/raquo/Laminar/issues/108

/**  The Map object holds key-value pairs and remembers the original insertion
  *  order of the keys. Any value (both objects and primitive values) may be used
  *  as either a key or a value.
  *
  *  @tparam K A type of key.
  *  @tparam V A type of value.
  *  @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map
  */
@js.native
@JSGlobal("Map")
private[raquo] class JsMap[K, V]() extends js.Object with js.Iterable[js.Tuple2[K, V]] {

  def this(@unused array: js.Iterable[js.Tuple2[K, V]]) = this()

  def has(@unused key: K): Boolean = js.native

  def keys(): js.Iterator[K] = js.native

  def set(@unused key: K, @unused value: V): js.Map[K, V] = js.native

  def get(@unused key: K): js.UndefOr[V] = js.native

  def clear(): Unit = js.native

  def delete(@unused key: K): Boolean = js.native

  @JSName(js.Symbol.iterator)
  override def jsIterator(): js.Iterator[js.Tuple2[K, V]] = js.native

  def size: Int = js.native
}


/** Factory for [[js.Map]] instances. */
object JsMap {

  /** Returns a new empty map */
  @inline def empty[K, V]: JsMap[K, V] = new JsMap[K, V]()

  def apply[K, V](properties: (K, V)*): JsMap[K, V] = {
    val map = empty[K, V]
    for (pair <- properties) {
      map.set(pair._1, pair._2)
    }
    map
  }
}
