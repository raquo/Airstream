package com.raquo.airstream.extensions

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.misc.{FilterStream, MapStream}

// #Warning do not edit this file directly, it is generated by GenerateTupleStreams.scala

// These mapN and filterN helpers are implicitly available on streams of tuples

class TupleStream2[T1, T2](val stream: EventStream[(T1, T2)]) extends AnyVal {

  def mapN[Out](project: (T1, T2) => Out): EventStream[Out] = {
    new MapStream[(T1, T2), Out](
      parent = stream,
      project = v => project(v._1, v._2),
      recover = None
    )
  }

  def filterN(passes: (T1, T2) => Boolean): EventStream[(T1, T2)] = {
    new FilterStream[(T1, T2)](
      parent = stream,
      passes = v => passes(v._1, v._2)
    )
  }
}

// --

class TupleStream3[T1, T2, T3](val stream: EventStream[(T1, T2, T3)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3) => Boolean): EventStream[(T1, T2, T3)] = {
    new FilterStream[(T1, T2, T3)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3)
    )
  }
}

// --

class TupleStream4[T1, T2, T3, T4](val stream: EventStream[(T1, T2, T3, T4)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4) => Boolean): EventStream[(T1, T2, T3, T4)] = {
    new FilterStream[(T1, T2, T3, T4)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4)
    )
  }
}

// --

class TupleStream5[T1, T2, T3, T4, T5](val stream: EventStream[(T1, T2, T3, T4, T5)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4, T5), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5) => Boolean): EventStream[(T1, T2, T3, T4, T5)] = {
    new FilterStream[(T1, T2, T3, T4, T5)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5)
    )
  }
}

// --

class TupleStream6[T1, T2, T3, T4, T5, T6](val stream: EventStream[(T1, T2, T3, T4, T5, T6)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4, T5, T6), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6)] = {
    new FilterStream[(T1, T2, T3, T4, T5, T6)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6)
    )
  }
}

// --

class TupleStream7[T1, T2, T3, T4, T5, T6, T7](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4, T5, T6, T7), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7)] = {
    new FilterStream[(T1, T2, T3, T4, T5, T6, T7)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7)
    )
  }
}

// --

class TupleStream8[T1, T2, T3, T4, T5, T6, T7, T8](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4, T5, T6, T7, T8), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7, T8) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    new FilterStream[(T1, T2, T3, T4, T5, T6, T7, T8)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8)
    )
  }
}

// --

class TupleStream9[T1, T2, T3, T4, T5, T6, T7, T8, T9](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out): EventStream[Out] = {
    new MapStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8, v._9),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    new FilterStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8, v._9)
    )
  }
}

// --

