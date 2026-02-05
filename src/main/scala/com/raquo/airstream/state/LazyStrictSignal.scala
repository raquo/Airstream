package com.raquo.airstream.state

import com.raquo.airstream.common.SingleParentSignal
import com.raquo.airstream.core.{Protected, Signal, Transaction}
import com.raquo.airstream.distinct.DistinctSignal
import com.raquo.airstream.misc.MapSignal

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
extends StrictSignal[O]
with SingleParentSignal[I, O] { self =>

  protected val displayNameSuffix: String

  override protected[this] val parent: Signal[I]

  protected def parentDisplayName: String = parent.displayName

  override protected def defaultDisplayName: String = parentDisplayName + displayNameSuffix + s"@${hashCode()}"

  override def tryNow(): Try[O] = {
    val newParentLastUpdateId = Protected.lastUpdateId(parent)
    // #TODO This comparison only works when parentSignal is started or strict.
    //  - e.g. it does not help us in `split`, it only helps us with lazyZoom.
    if (newParentLastUpdateId != _parentLastUpdateId) {
      // This branch can only run if !isStarted
      val nextValue = currentValueFromParent()
      updateCurrentValueFromParent(nextValue, newParentLastUpdateId)
      nextValue
    } else {
      super.tryNow()
    }
  }
}

object LazyStrictSignal {

  // #nc[lazy] expose LazyStrictSignal as .observeLazy()?

  def apply[A](
    parentSignal: Signal[A],
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): StrictSignal[A] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new LazyStrictSignal[A, A] {

      override protected val topoRank: Int = Protected.topoRank(parentSignal) + 1

      override protected[this] val parent: Signal[A] = parentSignal

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns

      override protected def onTry(nextParentValue: Try[A], transaction: Transaction): Unit = {
        super.onTry(nextParentValue, transaction)
        fireTry(nextParentValue, transaction)
      }

      override protected def currentValueFromParent(): Try[A] = parent.tryNow()
    }
  }

  def mapSignal[I, O](
    parentSignal: Signal[I],
    project: I => O,
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): MapSignal[I, O] with StrictSignal[O] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new MapSignal[I, O](
      parentSignal, project, recover = None
    ) with LazyStrictSignal[I, O] {

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns
    }
  }
  //
  // /** Convert any signal into a LazyStrictSignal */
  // def apply[A](
  //   parentSignal: Signal[A],
  //   parentDisplayName: => String, // parentSignal.displayName
  //   displayNameSuffix: String, // ".lazyStrictSignal"
  // ): StrictSignal[A] = {
  //   mapSignal[A, A](
  //     parentSignal, project = identity, parentDisplayName, displayNameSuffix
  //   )
  // }

  // #nc >>>> RESUME HERE >>> Why does this mapSignal implementation fails 9 tests, but the MapSignal implementation works?
  //  - Get to the bottom of this, something is sketchy.
  //  - If there's a contract I violated, I need to figure it out and document it.
  //  - Uncomment the below and run DistinctSpec / distinct signals – fail on line 196

  /** .map + make lazy-strict */
  // def mapSignal[I, O](
  //   parentSignal: Signal[I],
  //   project: I => O,
  //   parentDisplayName: => String,
  //   displayNameSuffix: String,
  // ): StrictSignal[O] = {
  //   val _pdn = parentDisplayName
  //   val _dns = displayNameSuffix
  //
  //   new LazyStrictSignal[I, O] {
  //
  //     override protected val topoRank: Int = Protected.topoRank(parentSignal) + 1
  //
  //     override protected[this] val parent: Signal[I] = parentSignal
  //
  //     override protected def parentDisplayName: String = _pdn
  //
  //     override protected val displayNameSuffix: String = _dns
  //
  //     override protected def onTry(nextParentValue: Try[I], transaction: Transaction): Unit = {
  //       super.onTry(nextParentValue, transaction)
  //       fireTry(nextParentValue.map(project), transaction)
  //     }
  //
  //     override protected def currentValueFromParent(): Try[O] = {
  //       parent.tryNow().map(project)
  //     }
  //   }
  // }

  def distinctSignal[A](
    parentSignal: Signal[A],
    isSame: (Try[A], Try[A]) => Boolean,
    resetOnStop: Boolean,
    parentDisplayName: => String,
    displayNameSuffix: String,
  ): DistinctSignal[A] with StrictSignal[A] = {
    val _pdn = parentDisplayName
    val _dns = displayNameSuffix

    new DistinctSignal[A](
      parentSignal, isSame, resetOnStop
    ) with LazyStrictSignal[A, A] {

      override protected def parentDisplayName: String = _pdn

      override protected val displayNameSuffix: String = _dns
    }
  }
}
