package com.raquo.airstream.status

import com.raquo.airstream.core.{BaseObservable, EventStream, Observable}

import scala.scalajs.js

/** Tracks the status of input and output of operator(stream). See [[Status]]. */
object AsyncStatusObservable {

  def apply[A, B, Self[+_] <: Observable[_]](
    parent: BaseObservable[Self, A],
    operator: Self[A] => EventStream[B]
  ): Self[Status[A, B]] = {
    // #TODO[Integrity] Are those var-s 100% safe?
    //  I think so, but it wouldn't hurt to test some weird transaction cases
    var ix = 0
    var maybeLastInput: js.UndefOr[A] = js.undefined

    val inputS = parent.map { input =>
      ix = 0
      maybeLastInput = input
      input
    }

    val outputS = operator(inputS).map { output =>
      ix += 1
      val lastInput = maybeLastInput.getOrElse(throw new Exception(s"${this}.asyncWithStatus: has output, but no input"))
      Resolved(lastInput, output, ix)
    }

    val pendingS = inputS.map(Pending(_))

    pendingS.matchStreamOrSignal(
      ifStream = _.mergeWith(outputS),
      ifSignal = _.composeChanges(_.mergeWith(outputS))
    ).asInstanceOf[Self[Status[A, B]]] // #TODO[Integrity] How to type this properly?
  }

}
