package com.raquo.airstream.extensions

import com.raquo.airstream.core.Observable
import com.raquo.airstream.flatten._

import scala.annotation.unused

class MetaObservable[A, Outer[+_] <: Observable[_], Inner[_]](
    val parent: Outer[Inner[A]]
) extends AnyVal {

  @inline def flatten[Output[+_] <: Observable[_]](implicit
      strategy: SwitchingStrategy[Outer, Inner, Output],
      @unused allowFlatMap: AllowFlatten
  ): Output[A] = {
    strategy.flatten(parent)
  }

  @inline def flattenSwitch[Output[+_] <: Observable[_]](implicit
      strategy: SwitchingStrategy[Outer, Inner, Output]
  ): Output[A] = {
    strategy.flatten(parent)
  }

  @inline def flattenMerge[Output[+_] <: Observable[_]](implicit
      strategy: MergingStrategy[Outer, Inner, Output]
  ): Output[A] = {
    strategy.flatten(parent)
  }

  @inline def flattenCustom[Output[+_] <: Observable[_]](
      strategy: FlattenStrategy[Outer, Inner, Output]
  ): Output[A] = {
    strategy.flatten(parent)
  }

}
