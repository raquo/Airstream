package com.raquo.airstream.combine.generated

import app.tulz.tuplez.Composition
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.core.Source.{EventSource, SignalSource}

// #Warning do not edit this file directly, it is generated by GenerateCombinableEventStream.scala

// These combine / combineWith / withCurrentValueOf / sample methods are implicitly available on all streams
// For combine / combineWith methods on the EventStream companion object, see StaticEventStreamCombineOps.scala

class CombinableEventStream[A](val stream: EventStream[A]) extends AnyVal {

  def combineWith[T1](
    s1: EventSource[T1]
  )(implicit c: Composition[A, (T1)]): EventStream[c.Composed] = {
    combineWithFn(s1)((a, v1) => c.compose(a, (v1)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, Out](
    s1: EventSource[T1]
  )(
    combinator: (A, T1) => Out
  ): EventStream[Out] = {
    new CombineEventStream2(stream, s1, combinator)
  }

  def withCurrentValueOf[T1](
    s1: SignalSource[T1]
  )(implicit c: Composition[A, (T1)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1) => c.compose(a, (v1))
    new SampleCombineEventStream2(stream, s1, combinator)
  }

  def sample[T1](
    s1: SignalSource[T1]
  ): EventStream[(T1)] = {
    new SampleCombineEventStream2[A, T1, (T1)](stream, s1, (_, v1) => (v1))
  }

  // --

  def combineWith[T1, T2](
    s1: EventSource[T1], s2: EventSource[T2]
  )(implicit c: Composition[A, (T1, T2)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2)((a, v1, v2) => c.compose(a, (v1, v2)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, Out](
    s1: EventSource[T1], s2: EventSource[T2]
  )(
    combinator: (A, T1, T2) => Out
  ): EventStream[Out] = {
    new CombineEventStream3(stream, s1, s2, combinator)
  }

  def withCurrentValueOf[T1, T2](
    s1: SignalSource[T1], s2: SignalSource[T2]
  )(implicit c: Composition[A, (T1, T2)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2) => c.compose(a, (v1, v2))
    new SampleCombineEventStream3(stream, s1, s2, combinator)
  }

  def sample[T1, T2](
    s1: SignalSource[T1], s2: SignalSource[T2]
  ): EventStream[(T1, T2)] = {
    new SampleCombineEventStream3[A, T1, T2, (T1, T2)](stream, s1, s2, (_, v1, v2) => (v1, v2))
  }

  // --

  def combineWith[T1, T2, T3](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3]
  )(implicit c: Composition[A, (T1, T2, T3)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3)((a, v1, v2, v3) => c.compose(a, (v1, v2, v3)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3]
  )(
    combinator: (A, T1, T2, T3) => Out
  ): EventStream[Out] = {
    new CombineEventStream4(stream, s1, s2, s3, combinator)
  }

  def withCurrentValueOf[T1, T2, T3](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3]
  )(implicit c: Composition[A, (T1, T2, T3)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3) => c.compose(a, (v1, v2, v3))
    new SampleCombineEventStream4(stream, s1, s2, s3, combinator)
  }

  def sample[T1, T2, T3](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3]
  ): EventStream[(T1, T2, T3)] = {
    new SampleCombineEventStream4[A, T1, T2, T3, (T1, T2, T3)](stream, s1, s2, s3, (_, v1, v2, v3) => (v1, v2, v3))
  }

  // --

  def combineWith[T1, T2, T3, T4](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4]
  )(implicit c: Composition[A, (T1, T2, T3, T4)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3, s4)((a, v1, v2, v3, v4) => c.compose(a, (v1, v2, v3, v4)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4]
  )(
    combinator: (A, T1, T2, T3, T4) => Out
  ): EventStream[Out] = {
    new CombineEventStream5(stream, s1, s2, s3, s4, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4]
  )(implicit c: Composition[A, (T1, T2, T3, T4)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4) => c.compose(a, (v1, v2, v3, v4))
    new SampleCombineEventStream5(stream, s1, s2, s3, s4, combinator)
  }

  def sample[T1, T2, T3, T4](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4]
  ): EventStream[(T1, T2, T3, T4)] = {
    new SampleCombineEventStream5[A, T1, T2, T3, T4, (T1, T2, T3, T4)](stream, s1, s2, s3, s4, (_, v1, v2, v3, v4) => (v1, v2, v3, v4))
  }

  // --

  def combineWith[T1, T2, T3, T4, T5](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3, s4, s5)((a, v1, v2, v3, v4, v5) => c.compose(a, (v1, v2, v3, v4, v5)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5]
  )(
    combinator: (A, T1, T2, T3, T4, T5) => Out
  ): EventStream[Out] = {
    new CombineEventStream6(stream, s1, s2, s3, s4, s5, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5) => c.compose(a, (v1, v2, v3, v4, v5))
    new SampleCombineEventStream6(stream, s1, s2, s3, s4, s5, combinator)
  }

  def sample[T1, T2, T3, T4, T5](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5]
  ): EventStream[(T1, T2, T3, T4, T5)] = {
    new SampleCombineEventStream6[A, T1, T2, T3, T4, T5, (T1, T2, T3, T4, T5)](stream, s1, s2, s3, s4, s5, (_, v1, v2, v3, v4, v5) => (v1, v2, v3, v4, v5))
  }

  // --

  def combineWith[T1, T2, T3, T4, T5, T6](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3, s4, s5, s6)((a, v1, v2, v3, v4, v5, v6) => c.compose(a, (v1, v2, v3, v4, v5, v6)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6) => Out
  ): EventStream[Out] = {
    new CombineEventStream7(stream, s1, s2, s3, s4, s5, s6, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6) => c.compose(a, (v1, v2, v3, v4, v5, v6))
    new SampleCombineEventStream7(stream, s1, s2, s3, s4, s5, s6, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6]
  ): EventStream[(T1, T2, T3, T4, T5, T6)] = {
    new SampleCombineEventStream7[A, T1, T2, T3, T4, T5, T6, (T1, T2, T3, T4, T5, T6)](stream, s1, s2, s3, s4, s5, s6, (_, v1, v2, v3, v4, v5, v6) => (v1, v2, v3, v4, v5, v6))
  }

  // --

  def combineWith[T1, T2, T3, T4, T5, T6, T7](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6], s7: EventSource[T7]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7)((a, v1, v2, v3, v4, v5, v6, v7) => c.compose(a, (v1, v2, v3, v4, v5, v6, v7)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6], s7: EventSource[T7]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7) => Out
  ): EventStream[Out] = {
    new CombineEventStream8(stream, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6, T7)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7) => c.compose(a, (v1, v2, v3, v4, v5, v6, v7))
    new SampleCombineEventStream8(stream, s1, s2, s3, s4, s5, s6, s7, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7)] = {
    new SampleCombineEventStream8[A, T1, T2, T3, T4, T5, T6, T7, (T1, T2, T3, T4, T5, T6, T7)](stream, s1, s2, s3, s4, s5, s6, s7, (_, v1, v2, v3, v4, v5, v6, v7) => (v1, v2, v3, v4, v5, v6, v7))
  }

  // --

  def combineWith[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6], s7: EventSource[T7], s8: EventSource[T8]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): EventStream[c.Composed] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8)((a, v1, v2, v3, v4, v5, v6, v7, v8) => c.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8)))
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, Out](
    s1: EventSource[T1], s2: EventSource[T2], s3: EventSource[T3], s4: EventSource[T4], s5: EventSource[T5], s6: EventSource[T6], s7: EventSource[T7], s8: EventSource[T8]
  )(
    combinator: (A, T1, T2, T3, T4, T5, T6, T7, T8) => Out
  ): EventStream[Out] = {
    new CombineEventStream9(stream, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def withCurrentValueOf[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8]
  )(implicit c: Composition[A, (T1, T2, T3, T4, T5, T6, T7, T8)]): EventStream[c.Composed] = {
    val combinator = (a: A, v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8) => c.compose(a, (v1, v2, v3, v4, v5, v6, v7, v8))
    new SampleCombineEventStream9(stream, s1, s2, s3, s4, s5, s6, s7, s8, combinator)
  }

  def sample[T1, T2, T3, T4, T5, T6, T7, T8](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8]
  ): EventStream[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    new SampleCombineEventStream9[A, T1, T2, T3, T4, T5, T6, T7, T8, (T1, T2, T3, T4, T5, T6, T7, T8)](stream, s1, s2, s3, s4, s5, s6, s7, s8, (_, v1, v2, v3, v4, v5, v6, v7, v8) => (v1, v2, v3, v4, v5, v6, v7, v8))
  }

  // --

}
