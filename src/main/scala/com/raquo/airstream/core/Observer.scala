package com.raquo.airstream.core

import com.raquo.airstream.core.AirstreamError.{ObserverError, ObserverErrorHandlingError}

import scala.util.{Failure, Success, Try}

trait Observer[-A] {

  /** Note: must not throw! */
  def onNext(nextValue: A): Unit

  /** Note: must not throw! */
  def onError(err: Throwable): Unit

  /** Note: must not throw! */
  def onTry(nextValue: Try[A]): Unit

  /** Creates another Observer such that calling its onNext will call this observer's onNext
    * with the value processed by the `project` function.
    *
    * This is useful when you need to pass down an Observer[A] to a child component
    * which should not know anything about the type A, but both child and parent know
    * about type `B`, and the parent knows how to translate B into A.
    *
    * @param project Note: guarded against exceptions
    */
  def contramap[B](project: B => A): Observer[B] = {
    Observer.withRecover(nextValue => onNext(project(nextValue)), {
      case nextError => onError(nextError)
    })
  }

  /** Creates another Observer such that calling its onNext will call this observer's onNext
    * with the same value, but only if it passes the test.
    *
    * @param passes Note: guarded against exceptions
    */
  def filter[B <: A](passes: B => Boolean): Observer[B] = {
    Observer.withRecover(nextValue => if (passes(nextValue)) onNext(nextValue), {
      case nextError => onError(nextError)
    })
  }
}

object Observer {

  /** @param onNext Note: guarded against exceptions */
  def apply[A](onNext: A => Unit): Observer[A] = {
    withRecover(onNext, onError = PartialFunction.empty)
  }

  /** @param onNext Note: guarded against exceptions */
  def ignoreErrors[A](onNext: A => Unit): Observer[A] = {
    withRecover(onNext, onError = { case _ => () })
  }

  /**
    * @param onNext Note: guarded against exceptions
    * @param onError Note: guarded against exceptions
    */
  def withRecover[A](onNext: A => Unit, onError: PartialFunction[Throwable, Unit]): Observer[A] = {
    val onNextParam = onNext // It's beautiful on the outside
    val onErrorParam = onError
    new Observer[A] {

      override final def onNext(nextValue: A): Unit = {
        // dom.console.log(s"===== Observer(${hashCode()}).onNext", nextValue.asInstanceOf[js.Any])
        try {
          onNextParam(nextValue)
        } catch {
          case err: Throwable => AirstreamError.sendUnhandledError(ObserverError(err))
        }
      }

      override final def onError(error: Throwable): Unit = {
        try {
          if (onErrorParam.isDefinedAt(error)) {
            onErrorParam(error)
          } else {
            AirstreamError.sendUnhandledError(error)
          }
        } catch {
          case err: Throwable => AirstreamError.sendUnhandledError(ObserverErrorHandlingError(error = err, cause = error))
        }
      }

      override final def onTry(nextValue: Try[A]): Unit = {
        nextValue.fold(onError, onNext)
      }
    }
  }

  /** @param onTry Note: guarded against exceptions */
  def fromTry[A](onTry: PartialFunction[Try[A], Unit]): Observer[A] = {
    val onTryParam = onTry

    new Observer[A] {

      override final def onNext(nextValue: A): Unit = {
        // dom.console.log(s"===== Observer(${hashCode()}).onNext", nextValue.asInstanceOf[js.Any])
        onTry(Success(nextValue))
      }

      override final def onError(error: Throwable): Unit = {
        onTry(Failure(error))
      }

      override final def onTry(nextValue: Try[A]): Unit = {
        try {
          if (onTryParam.isDefinedAt(nextValue)) {
            onTryParam(nextValue)
          } else {
            nextValue.fold(err => AirstreamError.sendUnhandledError(err), identity)
          }
        } catch {
          case err: Throwable =>
            nextValue.fold(
              originalError => AirstreamError.sendUnhandledError(ObserverErrorHandlingError(error = err, cause = originalError)),
              _ => AirstreamError.sendUnhandledError(ObserverError(err))
            )
        }
      }
    }
  }
}
