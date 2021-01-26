package com.raquo.airstream.debug

import com.raquo.airstream.core.EventStream

abstract class DebugEventStream[+A](debugger: ObservableDebugger[A]) extends EventStream[A] with DebugObservable[A] {

  override protected[this] def createDebugObservable(debugger: ObservableDebugger[A]): DebugEventStream[A] = {
    new DebugWriteEventStream[A](this, debugger)
  }

  override protected[this] def sourceName: String = debugger.sourceName
}
