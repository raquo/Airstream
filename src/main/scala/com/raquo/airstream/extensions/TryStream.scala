package com.raquo.airstream.extensions

import com.raquo.airstream.core.{EventStream, Signal}

import scala.util.{Failure, Success, Try}

/** See also [[TryObservable]] for generic try operators */
class TryStream[A](
  private val stream: EventStream[Try[A]]
) extends AnyVal {

  /** Emit `x` if parent stream emits `Right(x)`, do nothing otherwise */
  def collectSuccess: EventStream[A] = stream.collect { case Success(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Success(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectSuccess[C](pf: PartialFunction[A, C]): EventStream[C] = {
    stream.collectOpt(_.toOption.collect(pf))
  }

  /** Emit `x` if parent stream emits `Left(x)`, do nothing otherwise */
  def collectFailure: EventStream[Throwable] = stream.collect { case Failure(ev) => ev }

  /** Emit `pf(x)` if parent stream emits `Failure(x)` and `pf` is defined for `x`, do nothing otherwise */
  def collectFailure[C](pf: PartialFunction[Throwable, C]): EventStream[C] = {
    stream.collectOpt(_.toEither.left.toOption.collect(pf))
  }

}
