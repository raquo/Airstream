package com.raquo.airstream.basic.generated

import com.raquo.airstream.basic.MapSignal
import com.raquo.airstream.signal.Signal

// These mapN helpers are implicitly available on signals of tuples

class TupleSignal2[T1, T2](val signal: Signal[(T1, T2)]) extends AnyVal {

  def mapN[Out](project: (T1, T2) => Out): Signal[Out] = {
    new MapSignal[(T1, T2), Out](
      parent = signal,
      project = v => project(v._1, v._2),
      recover = None
    )
  }
}

// --

class TupleSignal3[T1, T2, T3](val signal: Signal[(T1, T2, T3)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3),
      recover = None
    )
  }
}

// --

class TupleSignal4[T1, T2, T3, T4](val signal: Signal[(T1, T2, T3, T4)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4),
      recover = None
    )
  }
}

// --

class TupleSignal5[T1, T2, T3, T4, T5](val signal: Signal[(T1, T2, T3, T4, T5)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4, T5), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4, v._5),
      recover = None
    )
  }
}

// --

class TupleSignal6[T1, T2, T3, T4, T5, T6](val signal: Signal[(T1, T2, T3, T4, T5, T6)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4, T5, T6), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6),
      recover = None
    )
  }
}

// --

class TupleSignal7[T1, T2, T3, T4, T5, T6, T7](val signal: Signal[(T1, T2, T3, T4, T5, T6, T7)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4, T5, T6, T7), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7),
      recover = None
    )
  }
}

// --

class TupleSignal8[T1, T2, T3, T4, T5, T6, T7, T8](val signal: Signal[(T1, T2, T3, T4, T5, T6, T7, T8)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4, T5, T6, T7, T8), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8),
      recover = None
    )
  }
}

// --

class TupleSignal9[T1, T2, T3, T4, T5, T6, T7, T8, T9](val signal: Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9)]) extends AnyVal {

  def mapN[Out](project: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out): Signal[Out] = {
    new MapSignal[(T1, T2, T3, T4, T5, T6, T7, T8, T9), Out](
      parent = signal,
      project = v => project(v._1, v._2, v._3, v._4, v._5, v._6, v._7, v._8, v._9),
      recover = None
    )
  }
}

// --

