package com.raquo.airstream.debug

import com.raquo.airstream.core.EventStream

/** This class exists for type inference purposes only (see "observable debugger type inference" test in DebugSpec),
  * the real meat is in [[DebuggableObservable]].
  */
class DebuggableStream[+A](override val observable: EventStream[A]) extends DebuggableObservable[EventStream, A](observable)
