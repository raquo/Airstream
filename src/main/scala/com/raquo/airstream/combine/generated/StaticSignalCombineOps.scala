package com.raquo.airstream.combine.generated

import com.raquo.airstream.core.Signal

// These combine and combineWith methods are available on the Signal companion object
// For instance methods of the same name, see CombinableSignal.scala

object StaticSignalCombineOps {

  def combine[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  ): Signal[(T1, T2)] = {
    combineWith(s1, s2)(Tuple2.apply[T1, T2])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, Out](
    s1: Signal[T1], s2: Signal[T2]
  )(
    combinator: (T1, T2) => Out
  ): Signal[Out] = {
    new CombineSignal2(s1, s2, combinator)
  }

  // --

  def combine[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  ): Signal[(T1, T2, T3)] = {
    combineWith(s1, s2, s3)(Tuple3.apply[T1, T2, T3])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  )(
    combinator: (T1, T2, T3) => Out
  ): Signal[Out] = {
    new CombineSignal3(s1, s2, s3, combinator)
  }

  // --

  def combine[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  ): Signal[(T1, T2, T3, T4)] = {
    combineWith(s1, s2, s3, s4)(Tuple4.apply[T1, T2, T3, T4])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  )(
    combinator: (T1, T2, T3, T4) => Out
  ): Signal[Out] = {
    new CombineSignal4(s1, s2, s3, s4, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  ): Signal[(T1, T2, T3, T4, T5)] = {
    combineWith(s1, s2, s3, s4, s5)(Tuple5.apply[T1, T2, T3, T4, T5])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  )(
    combinator: (T1, T2, T3, T4, T5) => Out
  ): Signal[Out] = {
    new CombineSignal5(s1, s2, s3, s4, s5, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  ): Signal[(T1, T2, T3, T4, T5, T6)] = {
    combineWith(s1, s2, s3, s4, s5, s6)(Tuple6.apply[T1, T2, T3, T4, T5, T6])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  )(
    combinator: (T1, T2, T3, T4, T5, T6) => Out
  ): Signal[Out] = {
    new CombineSignal6(s1, s2, s3, s4, s5, s6, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7)(Tuple7.apply[T1, T2, T3, T4, T5, T6, T7])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7) => Out
  ): Signal[Out] = {
    new CombineSignal7(s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8)(Tuple8.apply[T1, T2, T3, T4, T5, T6, T7, T8])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8) => Out
  ): Signal[Out] = {
    new CombineSignal8(s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8, s9)(Tuple9.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
  ): Signal[Out] = {
    new CombineSignal9(s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  // --

}
