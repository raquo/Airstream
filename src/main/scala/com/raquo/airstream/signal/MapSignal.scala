package com.raquo.airstream.signal

import com.raquo.airstream.core.MemoryObservable
import com.raquo.airstream.features.MapMemoryObservable

// @TODO[Elegance] Is this right that we shoved .recover into Map observables? Maybe it deserves its own class? But it's so many classes...

/** This signal emits an error if the parent observable emits an error or if `project` throws
  *
  * If `recover` is defined and needs to be called, it can do the following:
  * - Return Some(value) to make this signal emit value
  * - Return None to make this signal ignore (swallow) this error
  * - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
  *
  * @param project Note: guarded against exceptions
  * @param recover Note: guarded against exceptions
  */
class MapSignal[I, O](
  override protected[this] val parent: MemoryObservable[I],
  override protected[this] val project: I => O,
  override protected[this] val recover: Option[PartialFunction[Throwable, Option[O]]]
) extends Signal[O] with MapMemoryObservable[I, O] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1
}
