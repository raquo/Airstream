package com.raquo.airstream.combine.generated

import app.tulz.tuplez.Composition
import com.raquo.airstream.signal.Signal

private[airstream] trait CombineSignalOps[+A] { this: Signal[A] =>

  def combine[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  )(implicit composition: Composition[A, (T1, T2)]): Signal[composition.Composed] = {
    combineWith(s1, s2)((a, v1, v2) => composition.compose(a, (v1, v2)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, Out](
    s1: Signal[T1], s2: Signal[T2]
  )(
    combinator: (A, T1, T2) => Out
  ): Signal[Out] = {
    new CombineSignal3(this, s1, s2, combinator)
  }

  def withCurrentValueOf[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  )(implicit composition: Composition[A, (T1, T2)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2) => composition.compose(a, (v1, v2))
    new SampleCombineSignal3(this, s1, s2, combinator)
  }

  def sample[T1, T2](
    s1: Signal[T1], s2: Signal[T2]
  ): Signal[(T1, T2)] = {
    new SampleCombineSignal3[A, T1, T2, (T1, T2)](this, s1, s2, (_, v1, v2) => (v1, v2))
  }

  // --

  def combine[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  )(implicit composition: Composition[A, (T1, T2, T3)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3)((a, v1, v2, v3) => composition.compose(a, (v1, v2, v3)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  )(
    combinator: (A, T1, T2, T3) => Out
  ): Signal[Out] = {
    new CombineSignal4(this, s1, s2, s3, combinator)
  }

  def withCurrentValueOf[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  )(implicit composition: Composition[A, (T1, T2, T3)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3) => composition.compose(a, (v1, v2, v3))
    new SampleCombineSignal4(this, s1, s2, s3, combinator)
  }

  def sample[T1, T2, T3](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3]
  ): Signal[(T1, T2, T3)] = {
    new SampleCombineSignal4[A, T1, T2, T3, (T1, T2, T3)](this, s1, s2, s3, (_, v1, v2, v3) => (v1, v2, v3))
  }

  // --

  def combine[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  )(implicit composition: Composition[A, (T1, T2, T3, T4)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4)((a, v1, v2, v3, v4) => composition.compose(a, (v1, v2, v3, v4)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  )(
    combinator: (A, T1, T2, T3, T4) => Out
  ): Signal[Out] = {
    new CombineSignal5(this, s1, s2, s3, s4, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  )(implicit composition: Composition[A, (T1, T2, T3, T4)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4) => composition.compose(a, (v1, v2, v3, v4))
    new SampleCombineSignal5(this, s1, s2, s3, s4, combinator)
  }

  def sample[T1, T2, T3, T4](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4]
  ): Signal[(T1, T2, T3, T4)] = {
    new SampleCombineSignal5[A, T1, T2, T3, T4, (T1, T2, T3, T4)](this, s1, s2, s3, s4, (_, v1, v2, v3, v4) => (v1, v2, v3, v4))
  }

  // --

  def combine[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5)((a, v1, v2, v3, v4, v5) => composition.compose(a, (v1, v2, v3, v4, v5)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  )(
    combinator: (A, T1, T2, T3, T4, T5) => Out
  ): Signal[Out] = {
    new CombineSignal6(this, s1, s2, s3, s4, s5, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5) => composition.compose(a, (v1, v2, v3, v4, v5))
    new SampleCombineSignal6(this, s1, s2, s3, s4, s5, combinator)
  }

  def sample[T1, T2, T3, T4, T5](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5]
  ): Signal[(T1, T2, T3, T4, T5)] = {
    new SampleCombineSignal6[A, T1, T2, T3, T4, T5, (T1, T2, T3, T4, T5)](this, s1, s2, s3, s4, s5, (_, v1, v2, v3, v4, v5) => (v1, v2, v3, v4, v5))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6)((a, v1, v2, v3, v4, v5, v6) => composition.compose(a, (v1, v2, v3, v4, v5, v6)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6) => Out
  ): Signal[Out] = {
    new CombineSignal7(this, s1, s2, s3, s4, s5, s6, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6) => composition.compose(a, (v1, v2, v3, v4, v5, v6))
    new SampleCombineSignal7(this, s1, s2, s3, s4, s5, s6, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6]
  ): Signal[(T1, T2, T3, T4, T5, T6)] = {
    new SampleCombineSignal7[A, T1, T2, T3, T4, T5, T6, (T1, T2, T3, T4, T5, T6)](this, s1, s2, s3, s4, s5, s6, (_, v1, v2, v3, v4, v5, v6) => (v1, v2, v3, v4, v5, v6))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7)((a, v1, v2, v3, v4, v5, v6, v7) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7) => Out
  ): Signal[Out] = {
    new CombineSignal8(this, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7))
    new SampleCombineSignal8(this, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7)] = {
    new SampleCombineSignal8[A, T1, T2, T3, T4, T5, T6, T7, (T1, T2, T3, T4, T5, T6, T7)](this, s1, s2, s3, s4, s5, s6, s7, (_, v1, v2, v3, v4, v5, v6, v7) => (v1, v2, v3, v4, v5, v6, v7))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8)((a, v1, v2, v3, v4, v5, v6, v7, v8) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7, T8) => Out
  ): Signal[Out] = {
    new CombineSignal9(this, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8))
    new SampleCombineSignal9(this, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    new SampleCombineSignal9[A, T1, T2, T3, T4, T5, T6, T7, T8, (T1, T2, T3, T4, T5, T6, T7, T8)](this, s1, s2, s3, s4, s5, s6, s7, s8, (_, v1, v2, v3, v4, v5, v6, v7, v8) => (v1, v2, v3, v4, v5, v6, v7, v8))
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8, T9)]): Signal[composition.Composed] = {
    combineWith(s1, s2, s3, s4, s5, s6, s7, s8, s9)((a, v1, v2, v3, v4, v5, v6, v7, v8, v9) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8, v9)))
  }

  /** @param combinator Must not throw! */
  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8, T9, Out](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7, T8, T9) => Out
  ): Signal[Out] = {
    new CombineSignal10(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  )(implicit composition: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8, T9)]): Signal[composition.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9) => composition.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8, v9))
    new SampleCombineSignal10(this, s1, s2, s3, s4, s5, s6, s7, s8, s9, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    s1: Signal[T1], s2: Signal[T2], s3: Signal[T3], s4: Signal[T4], s5: Signal[T5], s6: Signal[T6], s7: Signal[T7], s8: Signal[T8], s9: Signal[T9]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    new SampleCombineSignal10[A, T1, T2, T3, T4, T5, T6, T7, T8, T9, (T1, T2, T3, T4, T5, T6, T7, T8, T9)](this, s1, s2, s3, s4, s5, s6, s7, s8, s9, (_, v1, v2, v3, v4, v5, v6, v7, v8, v9) => (v1, v2, v3, v4, v5, v6, v7, v8, v9))
  }

  // --

}
