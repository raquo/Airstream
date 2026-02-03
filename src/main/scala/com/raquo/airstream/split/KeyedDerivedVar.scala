package com.raquo.airstream.split

import com.raquo.airstream.state.{LazyDerivedVar2, StrictSignal, Var}

import scala.util.Try

class KeyedDerivedVar[K, ParentV, ThisV](
  val parent: Var[ParentV],
  override val signal: StrictSignal[ThisV],
  val key: K,
  updateParent: (Try[ParentV], Try[ThisV]) => Option[Try[ParentV]],
  displayNameSuffix: String
)
extends LazyDerivedVar2[ParentV, ThisV](
  parent = parent,
  signal = signal,
  updateParent = updateParent,
  displayNameSuffix = displayNameSuffix
)

object KeyedDerivedVar {

  @inline def standardErrorsF[M[_], A](
    updateParent: (M[A], A) => Option[M[A]]
  )(
    parentTry: Try[M[A]],
    nextTry: Try[A]
  ): Option[Try[M[A]]] = {
    LazyDerivedVar2.standardErrorsF(updateParent)(parentTry, nextTry)
  }

  // def noErrors[K, M[_], A](
  //   parent: Var[M[A]],
  //   signal: StrictSignal[A],
  //   key: K,
  //   updateParent: (M[A], A) => Option[M[A]],
  //   displayNameSuffix: String
  // ): KeyedDerivedVar[K, M, A] = {
  //   def fullUpdateParent(parentTry: Try[M[A]], nextTry: Try[A]): Option[Try[M[A]]] =
  //     parentTry match {
  //       case Success(parentValue) =>
  //         nextTry.map(updateParent(parentValue, _)) match {
  //           case Success(Some(result)) => Some(Success(result))
  //           case Success(None) => None
  //           case f: Failure[Nothing] => Some(f)
  //         }
  //       case f: Failure[Nothing] =>
  //         Some(f)
  //     }
  //   new KeyedDerivedVar(
  //     parent, signal, key, fullUpdateParent, displayNameSuffix
  //   )
  // }
}
