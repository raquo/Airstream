package com.raquo.airstream.util

import com.raquo.ew.JsArray

/** A `JsArray`-backed list with an iteration cursor ([[forEachExisting]])
  * that stays correct when the iteration callback mutates the list, i.e. when the
  * user code we run for each item adds or removes items of this same list.
  *
  * The contract of [[forEachExisting]] is:
  *  - The iteration range is snapshotted when the pass begins. Each item present at
  *    that moment, and not removed before the cursor reaches it, is visited
  *    exactly once, in order.
  *  - [[prepend]] and [[remove]] performed during the pass adjust the cursor so
  *    that no such item is skipped or visited twice, and so we never index out of
  *    bounds when the list shrinks.
  *  - An item [[append]]-ed during the pass lands beyond the snapshotted bound
  *    and is therefore not visited by that pass (a [[prepend]]-ed item is not
  *    visited either — see [[prepend]]). The caller is assumed to perform any
  *    necessary processing of such newly-added items itself.
  *
  * [[forEachExistingAndAppended]] behaves identically except that it DOES visit items
  * appended during the pass (after all originally-present items), which is handy when
  * the callback may add items that themselves need the same processing.
  *
  * @tparam A item type (e.g. `Subscription` or `DynamicSubscription`)
  */
class JsResilientIterator[A](
  // val ignoreRemovalsDuringIteration: Boolean   // #TODO possible performance optimization for Subscription case
) {

  private val items: JsArray[A] = JsArray()

  /** If we're currently iterating in [[forEachExisting]], this is the current item index. */
  private var iterIx = -1

  /** If we're currently iterating, this is the exclusive upper bound we'll iterate to.
    * Initially it matches items.length, and is adjusted for removing or prepending items during
    * iteration. It is extended for items appended during iteration ONLY when [[iterVisitsAppended]]
    * is true (i.e. under [[forEachExistingAndAppended]]); otherwise appended items are left beyond
    * the bound and not visited.
    */
  private var iterLength = -1

  /** Whether the current iteration should also visit items appended mid-pass.
    * Set by [[forEachExistingAndAppended]], cleared otherwise. Only meaningful while iterating.
    */
  private var iterVisitsAppended = false

  /** Whether we're currently iterating (in [[forEachExisting]] or [[forEachExistingAndAppended]]) */
  private def isIterating: Boolean =
    iterIx != -1

  @inline def length: Int = items.length

  /** Snapshot copy, for testing / introspection. */
  def toList: List[A] = items.asScalaJs.toList

  /** Append to the end of the list.
    *
    * If called during a [[forEachExisting]] pass, the new item is NOT visited by that
    * pass (it lands beyond the snapshotted bound); the caller is assumed to process it
    * itself. If called during a [[forEachExistingAndAppended]] pass, the new item IS
    * visited by that pass, after all originally-present items.
    */
  def append(item: A): Unit = {
    if (isIterating && iterVisitsAppended) {
      // Extend the iteration bound so this appended item is visited by the current pass.
      iterLength += 1
    }
    val _ = items.push(item)
  }

  /** Prepend to the start of the list.
    *
    * If called during a [[forEachExisting]] pass, the prepended item is NOT visited by
    * that pass: the cursor and bound are shifted to account for every existing
    * item moving one slot to the right, so the pass keeps visiting the same
    * logical (already-present) items.
    */
  def prepend(item: A): Unit = {
    if (isIterating) {
      iterIx += 1
      iterLength += 1
    }
    val _ = items.unshift(item)
  }

  /** Remove `item` from the list – safe even during iteration in [[forEachExisting]].
    *
    * @return true if the item was present (and removed), false otherwise.
    */
  def remove(item: A): Boolean = {
    val index = items.indexOf(item)
    if (index == -1) {
      false
    } else {
      if (isIterating) {
        // Removing an item within the pass's range shrinks the range.
        if (index < iterLength) {
          iterLength -= 1
        }
        // Removing an item at or before the cursor shifts the cursor left, so we
        // don't skip the item that takes the removed one's place.
        if (index <= iterIx) {
          iterIx -= 1
        }
      }
      items.splice(index, deleteCount = 1)
      true
    }
  }

  /** Remove all items at once, efficiently.
    * Must not be called during iteration in [[forEachExisting]] – will throw!
    */
  def clear(): Unit = {
    if (isIterating) {
      throw new Exception(s"Can not .clear() $this during iteration.")
    }
    items.length = 0
  }

  /** Iteration that tolerates the callback mutating this list via [[append]] / [[prepend]] / [[remove]].
    * Items [[append]]-ed during the pass are NOT visited (see the class comment).
    * The cursor is always reset, even if `cb` throws.
    */
  def forEachExisting(cb: A => Unit): Unit =
    iterate(visitAppended = false)(cb)

  /** Like [[forEachExisting]], but items [[append]]-ed during the pass ARE visited too,
    * in append order, after all originally-present items. Use this when the callback may
    * add items that themselves need the same processing – e.g. killing an Owner's
    * subscriptions, where a subscription's cleanup can register a new subscription that
    * must also be killed rather than left dangling.
    *
    * Note: [[prepend]]-ed items are still NOT visited (they land behind the cursor).
    * Note: if the callback keeps appending items without end, this will not terminate!
    */
  def forEachExistingAndAppended(cb: A => Unit): Unit =
    iterate(visitAppended = true)(cb)

  /** Raw, unsafe, iteration for legacy (pre-v18) behaviour only. */
  def forEachSnapshotLegacy(cb: A => Unit): Unit =
    items.forEach(cb)

  private def iterate(visitAppended: Boolean)(cb: A => Unit): Unit = {
    if (isIterating) {
      throw new Exception(s"Can not start iterating on $this: already iterating (this class is not re-entrant).")
    }
    iterVisitsAppended = visitAppended
    iterIx = 0
    iterLength = items.length
    try {
      while (iterIx < iterLength) {
        cb(items(iterIx))
        iterIx += 1
      }
    } finally {
      iterIx = -1
      iterLength = -1
      iterVisitsAppended = false
    }
  }
}
