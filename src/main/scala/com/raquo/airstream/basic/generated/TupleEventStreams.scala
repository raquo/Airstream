package com.raquo.airstream.basic.generated

import com.raquo.airstream.basic.{FilterEventStream, MapEventStream}
import com.raquo.airstream.eventstream.EventStream

// These mapN and filterN helpers are implicitly available on streams of tuples

class TupleEventStream2[T1, T2](val stream: EventStream[(T1, T2)]) extends AnyVal {

  def mapN[Out](project: (T1, T2) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2), Out](
      parent = stream,
      project = v => project(v._1, v._2),
      recover = None
    )
  }

  def filterN(passes: (T1, T2) => Boolean): EventStream[(T1, T2)] = {
    new FilterEventStream[(T1, T2)](
      parent = stream,
      passes = v => passes(v._1, v._2)
    )
  }
}

// --

class TupleEventStream3[T1, T2, T3](val stream: EventStream[(T1, T2, T3)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3) => Boolean): EventStream[(T1, T2, T3)] = {
    new FilterEventStream[(T1, T2, T3)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3)
    )
  }
}

// --

class TupleEventStream4[T1, T2, T3, T4](val stream: EventStream[(T1, T2, T3, T4)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4) => Boolean): EventStream[(T1, T2, T3, T4)] = {
    new FilterEventStream[(T1, T2, T3, T4)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4)
    )
  }
}

// --

class TupleEventStream5[T1, T2, T3, T4, T5](val stream: EventStream[(T1, T2, T3, T4, T5)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4, T5), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5) => Boolean): EventStream[(T1, T2, T3, T4, T5)] = {
    new FilterEventStream[(T1, T2, T3, T4, T5)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5)
    )
  }
}

// --

class TupleEventStream6[T1, T2, T3, T4, T5, T6](val stream: EventStream[(T1, T2, T3, T4, T5, T6)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4, T5, T6), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6)] = {
    new FilterEventStream[(T1, T2, T3, T4, T5, T6)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6)
    )
  }
}

// --

class TupleEventStream7[T1, T2, T3, T4, T5, T6, T7](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4, T5, T6, T7), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7)] = {
    new FilterEventStream[(T1, T2, T3, T4, T5, T6, T7)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7)
    )
  }
}

// --

class TupleEventStream8[T1, T2, T3, T4, T5, T6, T7, T8](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4, T5, T6, T7, T8), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7, T8) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    new FilterEventStream[(T1, T2, T3, T4, T5, T6, T7, T8)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8)
    )
  }
}

// --

class TupleEventStream9[T1, T2, T3, T4, T5, T6, T7, T8, T9](val stream: EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out): EventStream[Out] = {
    new MapEventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9), Out](
      parent = stream,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8, v._9),
      recover = None
    )
  }

  def filterN(passes: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Boolean): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    new FilterEventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](
      parent = stream,
      passes = v => passes(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8, v._9)
    )
  }
}

// --

