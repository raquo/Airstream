package com.raquo.airstream.fixtures

import com.raquo.airstream.core.Observer

import scala.collection.mutable

case class Effect[V](name: String, value: V)

object Effect {

  def log[V](name: String, to: mutable.Buffer[Effect[V]])(value: V): V = {
    val eff = Effect(name, value)
    to += eff
    value
  }

  def logObserver[V](name: String, to: mutable.Buffer[Effect[V]]): Observer[V] = {
    Observer[V](log(name, to))
  }
}
