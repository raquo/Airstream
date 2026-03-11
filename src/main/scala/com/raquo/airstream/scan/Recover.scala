package com.raquo.airstream.scan

import scala.util.{Failure, Success, Try}

/** A collection of strategies for handling errors in reduction operators such as `scanLeft`. */
private[scan] object Recover {

  /**
   * A binary operator that combines an accumulator of type `B` with the next value of type `A`.
   * For operators of this kind, the user is allowed to throw uncaught exceptions, and Airstream will catch them.
   */
  type Combine[A, B] = (B, A) => B

  /**
   * A binary operator that combines an accumulator of type `Try[B]` with the next value of type `Try[A]`.
   * For operators of this kind, the user is not allowed to throw uncaught exceptions, and must use [[Try]] instead.
   */
  type CombineTry[A, B] = Combine[Try[A], Try[B]]

  /**
   * A strategy for handling errors in a reduction operation.
   * Converts a [[Combine]] that can't handle errors into a [[CombineTry]] that can.
   */
  type RecoveryStrategy[A, B] = Combine[A, B] => CombineTry[A, B]

  /**
   * A reduction recovery strategy that keeps all errors.
   *
   * As soon as a single error occurs, the accumulator will remain blocked by an error state.
   */
  def keepErrors[A, B]: RecoveryStrategy[A, B] = combine => {
    case (Success(current), Success(next)) => Try(combine(current, next))
    case (Failure(error), _) => Failure(error)
    case (_, Failure(error)) => Failure(error)
  }

  /**
   * A reduction recovery strategy that skips upstream and combine errors but not seed errors.
   *
   * An event is silently ignored if it is a [[Failure]] or if it causes `combine` to throw an exception.
   * The accumulator simply maintains its previous state until a successful event arrives.
   */
  def skipErrors[A, B]: RecoveryStrategy[A, B] = combine => {
    case (Success(current), Success(next)) => Try(combine(current, next)).orElse(Success(current))
    case (Failure(error), _) => Failure(error)
    case (current, Failure(_)) => current
  }

  /**
   * A reduction recovery strategy that skips upstream errors but not seed or combine errors.
   *
   * An event is silently ignored if it is a [[Failure]].
   * The accumulator simply maintains its previous state until a successful event arrives.
   */
  def skipUpstreamErrors[A, B]: RecoveryStrategy[A, B] = combine => {
    case (Success(current), Success(next)) => Try(combine(current, next))
    case (Failure(error), _) => Failure(error)
    case (current, Failure(_)) => current
  }
}
