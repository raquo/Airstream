package com.raquo.airstream.core

import scala.scalajs.js

class ObserverList[Obs](private val observers: js.Array[Obs]) extends AnyVal {

  @inline def length: Int = observers.length

  @inline def apply(index: Int): Obs = observers(index)

  @inline def push(observer: Obs): Unit = {
    observers.push(observer)
    ()
  }

  /** @return whether observer was removed (`false` if it wasn't in the list) */
  def removeObserverNow(observer: Obs): Boolean = {
    val index = observers.indexOf(observer)
    val shouldRemove = index != -1
    if (shouldRemove) {
      observers.splice(index, deleteCount = 1)
    }
    shouldRemove
  }

  /** @param fn Must not throw */
  def foreach(fn: Obs => Unit): Unit = {
    var index = 0
    while (index < observers.length) {
      val observer = observers(index)
      index += 1 // Do this before invoking `fn` for a more graceful failure in case `fn` throws
      fn(observer)
    }
  }

}
