package com.raquo.airstream.combine.generated

import com.raquo.airstream.combine.CombineSignalN
import com.raquo.airstream.core.Signal
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.ew.JsArray

// Low-priority companion object methods: arities 10-22
// See CombineSignalObjectOps.scala for arities 2-9 (high-priority)

trait CombineSignalObjectOpsLow {

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10)(Tuple10.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11)(Tuple11.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12)(Tuple12.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13)(Tuple13.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14)(Tuple14.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15)(Tuple15.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16)(Tuple16.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17)(Tuple17.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18)(Tuple18.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable, s18.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
        arr(17).asInstanceOf[T18],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19)(Tuple19.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable, s18.toObservable, s19.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
        arr(17).asInstanceOf[T18],
        arr(18).asInstanceOf[T19],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20)(Tuple20.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable, s18.toObservable, s19.toObservable, s20.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
        arr(17).asInstanceOf[T18],
        arr(18).asInstanceOf[T19],
        arr(19).asInstanceOf[T20],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20], s21: SignalSource[T21]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20, s21)(Tuple21.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20], s21: SignalSource[T21]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable, s18.toObservable, s19.toObservable, s20.toObservable, s21.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
        arr(17).asInstanceOf[T18],
        arr(18).asInstanceOf[T19],
        arr(19).asInstanceOf[T20],
        arr(20).asInstanceOf[T21],
      )
    )
  }

  // --

  def combine[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20], s21: SignalSource[T21], s22: SignalSource[T22]
  ): Signal[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] = {
    combineWithFn(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20, s21, s22)(Tuple22.apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22])
  }

  /** @param combinator Must not throw! */
  def combineWithFn[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, Out](
    s1: SignalSource[T1], s2: SignalSource[T2], s3: SignalSource[T3], s4: SignalSource[T4], s5: SignalSource[T5], s6: SignalSource[T6], s7: SignalSource[T7], s8: SignalSource[T8], s9: SignalSource[T9], s10: SignalSource[T10], s11: SignalSource[T11], s12: SignalSource[T12], s13: SignalSource[T13], s14: SignalSource[T14], s15: SignalSource[T15], s16: SignalSource[T16], s17: SignalSource[T17], s18: SignalSource[T18], s19: SignalSource[T19], s20: SignalSource[T20], s21: SignalSource[T21], s22: SignalSource[T22]
  )(
    combinator: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22) => Out
  ): Signal[Out] = {
    new CombineSignalN[Any, Out](
      parents = JsArray(s1.toObservable, s2.toObservable, s3.toObservable, s4.toObservable, s5.toObservable, s6.toObservable, s7.toObservable, s8.toObservable, s9.toObservable, s10.toObservable, s11.toObservable, s12.toObservable, s13.toObservable, s14.toObservable, s15.toObservable, s16.toObservable, s17.toObservable, s18.toObservable, s19.toObservable, s20.toObservable, s21.toObservable, s22.toObservable),
      combinator = arr => combinator(
        arr(0).asInstanceOf[T1],
        arr(1).asInstanceOf[T2],
        arr(2).asInstanceOf[T3],
        arr(3).asInstanceOf[T4],
        arr(4).asInstanceOf[T5],
        arr(5).asInstanceOf[T6],
        arr(6).asInstanceOf[T7],
        arr(7).asInstanceOf[T8],
        arr(8).asInstanceOf[T9],
        arr(9).asInstanceOf[T10],
        arr(10).asInstanceOf[T11],
        arr(11).asInstanceOf[T12],
        arr(12).asInstanceOf[T13],
        arr(13).asInstanceOf[T14],
        arr(14).asInstanceOf[T15],
        arr(15).asInstanceOf[T16],
        arr(16).asInstanceOf[T17],
        arr(17).asInstanceOf[T18],
        arr(18).asInstanceOf[T19],
        arr(19).asInstanceOf[T20],
        arr(20).asInstanceOf[T21],
        arr(21).asInstanceOf[T22],
      )
    )
  }

  // --

}
