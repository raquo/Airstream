package com.raquo.airstream.status

import com.raquo.airstream.core.{BaseObservable, EventStream, Observable}

object FlatMapStatusObservable {

  def apply[A, B, Self[+_] <: Observable[_]](
    parent: BaseObservable[Self, A],
    project: A => EventStream[B]
  ): Self[Status[A, B]] = {
    // #TODO[Integrity] Not sure how to type this properly
    val parentObservable = parent.asInstanceOf[Observable[A] with BaseObservable[Self, A]]

    var ix = 0

    val inputS = parentObservable.map { input =>
      ix = 0
      Pending[A](input)
    }

    val outputS = inputS.flatMapSwitch { pending =>
      project(pending.input).map { output =>
        ix += 1
        Resolved(pending.input, output, ix)
      }
    }

    inputS.matchStreamOrSignal(
      ifStream = _.mergeWith(outputS),
      ifSignal = _.updates(_.mergeWith(outputS))
    ).asInstanceOf[Self[Status[A, B]]] // #TODO[Integrity] How to type this properly? matchStreamOrSignalAsSelf is not enough...
  }

}
