package com.raquo.airstream.compose

import com.raquo.airstream.common.{InternalTryObserver, SingleParentSignal}
import com.raquo.airstream.compose.ComposeEitherSignal.NoValueError
import com.raquo.airstream.core.{AirstreamError, Protected, Signal, Transaction, WritableSignal}
import com.raquo.ew.JsArray

import scala.util.Try

class ComposeEitherSignal[A, B, A2, B2](
  parent: Signal[Either[A, B]],
  makeLeftSignal: Signal[A] => Signal[A2],
  makeRightSignal: Signal[B] => Signal[B2]
) extends WritableSignal[Either[A2, B2]] {

  // #TODO
  //  We could make a FilteredSignal class that would just skip updates that don't match the predicate,
  //  and... avoid... initializing... the initial value, somehow
  //  >>> Or maybe we should just implement all this based on streams?

  private val leftInputSignal: Signal[A] = parent.map(_.swap.getOrElse(throw new NoValueError))

  private val rightInputSignal: Signal[B] = parent.map(_.getOrElse(throw new NoValueError))

  private val leftOutputSignal: Signal[A2] = makeLeftSignal(leftInputSignal)

  private val rightOutputSignal: Signal[B2] = makeRightSignal(rightInputSignal)

  private val parents = JsArray(parent, leftOutputSignal, rightOutputSignal)

  override protected val topoRank: Int = Protected.maxTopoRank(parents) + 1

  // #TODO
  //  - leftInputSignal and rightInputSignal
  //    - Should be started when this signal is started... I think? Or not?
  //    - Add internal observers to them here
  //    - Note that output signals might in theory not depend on them...
  //  - Output
  //    - leftOutputSignal and rightOutputSignal should be merged, with logic similar to MergeEventStream
  //    - basically we add internal observers to them, and re-emit their updates
  //    - may need to filter out NoValueError?
  //  - Pulling value from parent
  //    - including initial value
  //    - look at parent.now(), if it's right(), get the value of rightOutputSignal, otherwise, get the value of leftOutputSignal
  //    - will that actually work? I'm not sure

  // override protected def onTry(
  //   nextValue: Try[Either[A, B]],
  //   transaction: Transaction
  // ): Unit = {
  //   nextValue.fold(
  //     nextErr => ???,
  //     nextEither => nextEither.fold(
  //       leftInput => leftInputSignal.emit(leftInput),
  //       rightInput => rightInputSignal.emit(rightInput),
  //     )
  //   )
  // }

  // private val metaSignal = parent.splitEither(
  //   (_, leftSignal) => makeLeftSignal(leftSignal).map(Right(_)),
  //   (_, rightSignal) => makeRightSignal(rightSignal).map(Left(_)),
  // )

  override protected def onWillStart(): Unit = {
    // #TODO
    ???
  }

  override protected def currentValueFromParent(): Try[Either[A2, B2]] = {
    ???
  }
}

object ComposeEitherSignal {

  // #TODO Dunno about this. disable stack trace or something
  class NoValueError() extends Throwable
}
