package com.raquo.airstream.state

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Protected, Signal, Transaction}
import com.raquo.airstream.debug.{Debugger, DebuggerSignal}
import com.raquo.airstream.distinct.DistinctSignal

import scala.util.Try

/** #TODO[Naming,Org] Messy
  *
  * This signal offers the API of a [[StrictSignal]] but is actually
  * lazy. All it does is let you PULL the signal's current value.
  * This mostly works fine if your signal does not depend on any streams.
  *
  * We use this signal internally for derived Var use cases where we know
  * that it should work fine. we may change the naming and structure of
  * this class when we implement settle on a long term strategy for
  * peekNow / pullNow https://github.com/raquo/Laminar/issues/130
  */
trait LazyStrictSignal[I, O]
extends WritableStrictSignal[O]
with SingleParentSignal[I, O] {

  protected val displayNameSuffix: String

  override protected[this] val parent: Signal[I]

  protected def parentDisplayName: String = parent.displayName

  override protected def defaultDisplayName: String = parentDisplayName + displayNameSuffix + s"@${hashCode()}"

  override def tryNow(): Try[O] = {
    // dom.console.log(s"> ${this} > tryNow")
    if (parent.isInstanceOf[LazyStrictSignal[_, _]]) {
      // LazyStrictSignal-s are lazy, so if we want to actively pull a fresh value
      // from a LazyStrictSignal that depends on another LazyStrictSignal, we need
      // to force-evaluate the current value of that parent signal.
      // We need to do this recursively until we reach a (typically) StrictSignal
      // parent, then we can check if that strict parent has updated since we last
      // checked, and if that's the case, we can pull its value down this chain.
      // And once we force-evaluate the parent signal's value,
      // `peekWhetherParentHasUpdated` will return true if the parent has updated.
      // All this is assuming no observers on any of the signals involved.
      parent.tryNow()
    }
    if (peekWhetherParentHasUpdated().contains(true)) {
      val nextValue = currentValueFromParent()
      updateCurrentValueFromParent(
        nextValue,
        Protected.lastUpdateId(parent)
      )
      nextValue
    } else {
      super.tryNow()
    }
  }
}

object LazyStrictSignal {

  // #nc[lazy] expose LazyStrictSignal as .observeLazy()?

  /** Convert any signal into a LazyStrictSignal */
  def apply[A](
    parentSignal: Signal[A],
    parentDisplayName: => String, // parentSignal.displayName
    displayNameSuffix: String, // ".lazyStrictSignal"
  ): StrictSignal[A] = {
    mapSignal[A, A](
      parentSignal, project = identity, parentDisplayName, displayNameSuffix
    )
  }

  /** .map + make lazy-strict */
  def mapSignal[I, O](
    parentSignal: Signal[I],
    project: I => O,
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): StrictSignal[O] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new LazyStrictSignal[I, O] {

      override protected val topoRank: Int = Protected.topoRank(parentSignal) + 1

      override protected[this] val parent: Signal[I] = parentSignal

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns

      override protected[this] def displayClassName: String = s"LazyStrictSignal{}"

      override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
        super.onTry(nextParentValue, transaction)
        fireTry(nextParentValue.map(project), transaction)
      }

      override protected def currentValueFromParent(): Try[O] = {
        parent.tryNow().map(project)
      }
    }
  }

  def distinctSignal[A](
    parentSignal: Signal[A],
    isSame: (Try[A], Try[A]) => Boolean,
    resetOnStop: Boolean,
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): DistinctSignal[A] with StrictSignal[A] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new DistinctSignal[A](parentSignal, isSame, resetOnStop)
      with LazyStrictSignal[A, A] {

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns

      override protected[this] def displayClassName: String = s"DistinctSignal+LazyStrictSignal"
    }
  }

  def debuggerSignal[A](
    parentSignal: Signal[A],
    debugger: Debugger[A],
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): DebuggerSignal[A] with StrictSignal[A] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new DebuggerSignal[A](parentSignal, debugger)
      with LazyStrictSignal[A, A] {

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns

      override protected[this] def displayClassName: String = "DebuggerSignal+LazyStrictSignal"
    }
  }
}
