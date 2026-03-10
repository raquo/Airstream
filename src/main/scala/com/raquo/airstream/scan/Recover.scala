package com.raquo.airstream.scan

import scala.util.{Failure, Success, Try}

/** A collection of strategies for handling errors in reduction operators such as `scanLeft`. */
private[scan] object Recover {

  /**
   * A binary operator that combines an accumulator of type `B` with the next value of type `A`.
   *
   * @note When passing a [[Combine]] operation, the user is allowed to throw uncaught exceptions.
   *       Airstream will catch these and process them using [[Try]].
   * @see  [[CombineTry]] is a variant with explicit error handling.
   */
  type Combine[A, B] = (B, A) => B

  /**
   * A binary operator that combines an accumulator of type `Try[B]` with the next value of type `Try[A]`.
   *
   * Everything is wrapped with [[Try]] to support explicit error handling in the operator itself.
   * An implementation can decide when to keep errors and when/how to recover from them.
   * 
   * @note When passing a [[CombineTry]] operation, it is expected that the user will handle all errors with [[Try]],
   *       with no exceptions remaining uncaught. Violation of this assumption will likely cause Airstream to crash.
   * @note Can be created from a regular [[Combine]] operation using a [[RecoveryStrategy]].
   * @see  [[Combine]] is a variant without error handling.
   */
  type CombineTry[A, B] = Combine[Try[A], Try[B]]

  /**
   * A strategy for handling errors in a reduction operation.
   * There are three kinds of error that a strategy must consider:
   *   - Upstream Errors: Those originating from the parent or another ancestor.
   *   - Seed Errors: Those originating from computation of the initial seed value.
   *   - Combine Errors: Those originating from the combine function.
   *
   * Mechanically, a strategy is a functional that transforms a simple [[Combine]] reduction
   * operator with no error handling into a [[CombineTry]] variant that can handle errors.
   */
  type RecoveryStrategy[A, B] = Combine[A, B] => CombineTry[A, B]

  /**
   * A reduction recovery strategy that keeps all errors.
   *
   * As soon as a single error occurs, the accumulator will remain blocked by an error state.
   */
  @inline def keepErrors[A, B]: RecoveryStrategy[A, B] = combine => {
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
  @inline def skipErrors[A, B]: RecoveryStrategy[A, B] = combine => {
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
  @inline def skipUpstreamErrors[A, B]: RecoveryStrategy[A, B] = combine => {
    case (Success(current), Success(next)) => Try(combine(current, next))
    case (Failure(error), _) => Failure(error)
    case (current, Failure(_)) => current
  }
}
