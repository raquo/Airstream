package com.raquo.airstream.combine.generated

import com.raquo.airstream.eventstream.EventStream

private[airstream] trait EventStreamCombineMethods {

  def combine[T1, T2, T3](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3]
  ): EventStream[(T1, T2, T3)] = {
    combineWith(s1, s2, s3)(Tuple3.apply[T1, T2, T3])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3]
  )(
    combinator: (T1, T2, T3) => Out
  ): EventStream[Out] = {
    new CombineEventStream3(s1, s2, s3, combinator)
  }

  // --

  def combine[T1, T2, T3, T4](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4]
  ): EventStream[(T1, T2, T3, T4)] = {
    combineWith(s1, s2, s3, s4)(Tuple4.apply[T1, T2, T3, T4])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4]
  )(
    combinator: (T1, T2, T3, T4) => Out
  ): EventStream[Out] = {
    new CombineEventStream4(s1, s2, s3, s4, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5]
  ): EventStream[(T1, T2, T3, T4, T5)] = {
    combineWith(s1, s2, s3, s4, s5)(Tuple5.apply[T1, T2, T3, T4, T5])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5]
  )(
    combinator: (T1, T2, T3, T4, T5) => Out
  ): EventStream[Out] = {
    new CombineEventStream5(s1, s2, s3, s4, s5, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6]
  ): EventStream[(T1, T2, T3, T4, T5, T6)] = {
    combineWith(s1, s2, s3, s4, s5, s6)(Tuple6.apply[T1, T2, T3, T4, T5, T6])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6]
  )(
    combinator: (T1, T2, T3, T4, T5, T6) => Out
  ): EventStream[Out] = {
    new CombineEventStream6(s1, s2, s3, s4, s5, s6, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7)(Tuple7.apply[T1, T2, T3, T4, T5, T6, T7])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7) => Out
  ): EventStream[Out] = {
    new CombineEventStream7(s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8)(Tuple8.apply[T1, T2, T3, T4, T5, T6, T7, T8])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8) => Out
  ): EventStream[Out] = {
    new CombineEventStream8(s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8, s9)(Tuple9.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
  ): EventStream[Out] = {
    new CombineEventStream9(s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9], s10: EventStream[T10]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10)(Tuple10.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10])
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9], s10: EventStream[T10]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => Out
  ): EventStream[Out] = {
    new CombineEventStream10(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, combinator)
  }

  // --

}
