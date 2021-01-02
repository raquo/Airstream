package com.raquo.airstream.combine.generated

import app.tulz.tuplez.Composition
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal

private[airstream] trait CombineEventStreamOps[+A] { this: EventStream[A] =>

  def combine[T1, T2](
    s1: EventStream[T1], s2: EventStream[T2]
  )(implicit composition: Composition[A, (T1, T2)]): EventStream[composition.Composed] = {
    combineWith(s1, s2)((a, v1, v2) => composition.compose(a, (v1, v2)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, Out](
    s1: EventStream[T1], s2: EventStream[T2]
  )(
    combinator: (A, T1, T2) => Out
  ): EventStream[Out] = {
    new CombineEventStream3(this, s1, s2, combinator)
  }

  def withCurrentValueOf[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  )(implicit composition: Composition[A, (T1, T2)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2) => composition.compose(a, (v1, v2))
    new SampleCombineEventStream3(this, s1, s2, combinator)
  }

  def sample[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  ): EventStream[(T1, T2)] = {
    new SampleCombineEventStream3[A, T1, T2, (T1, T2)](this, s1, s2, (_, v1, v2) => (v1, v2))
  }

  // --

  def combine[T1, T2, T3](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3]
  )(implicit composition: Composition[A, (T1, T2, T3)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3)((a, v1, v2, v3) => composition.compose(a, (v1, v2, v3)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3]
  )(
    combinator: (A, T1, T2, T3) => Out
  ): EventStream[Out] = {
    new CombineEventStream4(this, s1, s2, s3, combinator)
  }

  def withCurrentValueOf[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  )(implicit composition: Composition[A, (T1, T2, T3)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3) => composition.compose(a, (v1, v2, v3))
    new SampleCombineEventStream4(this, s1, s2, s3, combinator)
  }

  def sample[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  ): EventStream[(T1, T2, T3)] = {
    new SampleCombineEventStream4[A, T1, T2, T3, (T1, T2, T3)](this, s1, s2, s3, (_, v1, v2, v3) => (v1, v2, v3))
  }

  // --

  def combine[T1, T2, T3, T4](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4]
  )(implicit composition: Composition[A, (T1, T2, T3, T4)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4)((a, v1, v2, v3, v4) => composition.compose(a, (v1, v2, v3, v4)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4]
  )(
    combinator: (A, T1, T2, T3, T4) => Out
  ): EventStream[Out] = {
    new CombineEventStream5(this, s1, s2, s3, s4, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  )(implicit composition: Composition[A, (T1, T2, T3, T4)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4) => composition.compose(a, (v1, v2, v3, v4))
    new SampleCombineEventStream5(this, s1, s2, s3, s4, combinator)
  }

  def sample[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  ): EventStream[(T1, T2, T3, T4)] = {
    new SampleCombineEventStream5[A, T1, T2, T3, T4, (T1, T2, T3, T4)](this, s1, s2, s3, s4, (_, v1, v2, v3, v4) => (v1, v2, v3, v4))
  }

  // --

  def combine[T1, T2, T3, T4, T5](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5)((a, v1, v2, v3, v4, v5) => composition.compose(a, (v1, v2, v3, v4, v5)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5]
  )(
    combinator: (A, T1, T2, T3, T4, T5) => Out
  ): EventStream[Out] = {
    new CombineEventStream6(this, s1, s2, s3, s4, s5, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5) => composition.compose(a, (v1, v2, v3, v4, v5))
    new SampleCombineEventStream6(this, s1, s2, s3, s4, s5, combinator)
  }

  def sample[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  ): EventStream[(T1, T2, T3, T4, T5)] = {
    new SampleCombineEventStream6[A, T1, T2, T3, T4, T5, (T1, T2, T3, T4, T5)](this, s1, s2, s3, s4, s5, (_, v1, v2, v3, v4, v5) => (v1, v2, v3, v4, v5))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6)((a, v1, v2, v3, v4, v5, v6) => composition.compose(a, (v1, v2, v3, v4, v5, v6)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6) => Out
  ): EventStream[Out] = {
    new CombineEventStream7(this, s1, s2, s3, s4, s5, s6, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6) => composition.compose(a, (v1, v2, v3, v4, v5, v6))
    new SampleCombineEventStream7(this, s1, s2, s3, s4, s5, s6, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  ): EventStream[(T1, T2, T3, T4, T5, T6)] = {
    new SampleCombineEventStream7[A, T1, T2, T3, T4, T5, T6, (T1, T2, T3, T4, T5, T6)](this, s1, s2, s3, s4, s5, s6, (_, v1, v2, v3, v4, v5, v6) => (v1, v2, v3, v4, v5, v6))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7)((a, v1, v2, v3, v4, v5, v6, v7) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7) => Out
  ): EventStream[Out] = {
    new CombineEventStream8(this, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7))
    new SampleCombineEventStream8(this, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7)] = {
    new SampleCombineEventStream8[A, T1, T2, T3, T4, T5, T6, T7, (T1, T2, T3, T4, T5, T6, T7)](this, s1, s2, s3, s4, s5, s6, s7, (_, v1, v2, v3, v4, v5, v6, v7) => (v1, v2, v3, v4, v5, v6, v7))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8)((a, v1, v2, v3, v4, v5, v6, v7, v8) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7, T8) => Out
  ): EventStream[Out] = {
    new CombineEventStream9(this, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8))
    new SampleCombineEventStream9(this, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    new SampleCombineEventStream9[A, T1, T2, T3, T4, T5, T6, T7, T8, (T1, T2, T3, T4, T5, T6, T7, T8)](this, s1, s2, s3, s4, s5, s6, s7, s8, (_, v1, v2, v3, v4, v5, v6, v7, v8) => (v1, v2, v3, v4, v5, v6, v7, v8))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8, T9)]): EventStream[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8, s9)((a, v1, v2, v3, v4, v5, v6, v7, v8, v9) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8, v9)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
    s1: EventStream[T1], s2: EventStream[T2], s3: EventStream[T3], s4: EventStream[T4], s5: EventStream[T5], s6: EventStream[T6], s7: EventStream[T7], s8: EventStream[T8], s9: EventStream[T9]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
  ): EventStream[Out] = {
    new CombineEventStream10(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8, T9)]): EventStream[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8, v9))
    new SampleCombineEventStream10(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    new SampleCombineEventStream10[A, T1, T2, T3, T4, T5, T6, T7, T8, T9, (T1, T2, T3, T4, T5, T6, T7, T8, T9)](this, s1, s2, s3, s4, s5, s6, s7, s8, s9, (_, v1, v2, v3, v4, v5, v6, v7, v8, v9) => (v1, v2, v3, v4, v5, v6, v7, v8, v9))
  }

  // --

}
