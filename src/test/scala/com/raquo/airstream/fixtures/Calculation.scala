package com.raquo.airstream.fixtures

import scala.collection.mutable

case class Calculation[V](name: String, value: V)

object Calculation {

  def log[V](name: String, to: mutable.Buffer[Calculation[V]])(value: V): V = {
    val calculation = Calculation(name, value)
    // println(calculation)
    to += calculation
    value
  }
}
