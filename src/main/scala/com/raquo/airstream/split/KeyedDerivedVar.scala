package com.raquo.airstream.split

import com.raquo.airstream.state.{LazyDerivedVar, StrictSignal, Var}

import scala.util.Try

class KeyedDerivedVar[K, ParentV, ThisV](
  parent: Var[ParentV],
  override val signal: StrictSignal[ThisV],
  override val key: K,
  updateParent: (Try[ParentV], Try[ThisV]) => Option[Try[ParentV]],
  displayNameSuffix: String
)
extends LazyDerivedVar[ParentV, ThisV](
  parent = parent,
  signal = signal,
  updateParent = updateParent,
  displayNameSuffix = displayNameSuffix
)
with Keyed[K]

object KeyedDerivedVar {

  @inline def standardErrorsF[M[_], A](
    updateParent: (M[A], A) => Option[M[A]]
  )(
    parentTry: Try[M[A]],
    nextTry: Try[A]
  ): Option[Try[M[A]]] = {
    LazyDerivedVar.standardErrorsF(updateParent)(parentTry, nextTry)
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
