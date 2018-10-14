package com.raquo.airstream.state

import com.raquo.airstream.core.MemoryObservable
import com.raquo.airstream.features.MapMemoryObservable
import com.raquo.airstream.ownership.Owner

/** This state emits an error if the parent observable emits an error or if `project` throws
  *
  * If `recover` is defined and needs to be called, it can do the following:
  * - Return `Some(value)` to make this state emit `value`
  * - Return `None` to make this state ignore (swallow) this error
  * - Not handle the error (meaning .isDefinedAt(error) must be false) to emit the original error
  *
  * @param project Note: guarded against exceptions
  * @param recover Note: guarded against exceptions
  */
class MapState[I, O](
  override protected[this] val parent: MemoryObservable[I],
  override protected[this] val project: I => O,
  override protected[this] val recover: Option[PartialFunction[Throwable, Option[O]]],
  override protected[state] val owner: Owner
) extends State[O] with MapMemoryObservable[I, O] {

  override protected[airstream] val topoRank: Int = parent.topoRank + 1

  init()

  onStart()
}
