package com.raquo.airstream.core

import scala.scalajs.js

/**
  * A Sink is something that can be converted to an [[Observer]].
  * The counterparty to Sink is a [[Source]], something that can be converted to an [[Observable]].
  *
  * A Sink could be an Observer itself, an EventBus, a Var, or, via implicits, an external type like js.Function1.
  *
  * The point of using Sink instead of Observer in your API is to let the end users
  * pass simply `eventBus` instead of `eventBus.writer` to a method that requires Sink,
  * and to achieve that without having an implicit conversion from EventBus to Observer,
  * because then you'd also want an implicit conversion from EventBus to Observable, and
  * those two would be incompatible (e.g. both Observable and Observer have a filter method).
  */
trait Sink[-A] {
  def toObserver: Observer[A]
}


object Sink {

  // @TODO[Scala3]
  //  - Unfortunately I can't get callbackToSink to work in Laminar because the type inference
  //    fails if you provide a lambda (without type ascription) like (v => println(v)) where
  //    a Sink[String] is expected.
  //  - Check this again in Scala 3

  //implicit def callbackToSink[A](callback: A => Unit): Sink[A] = new Sink[A] {
  //  override def toObserver: Observer[A] = Observer(callback)
  //}

  implicit def jsCallbackToSink[A](callback: js.Function1[A, Unit]): Sink[A] = new Sink[A] {
    override def toObserver: Observer[A] = Observer(callback)
  }
}
