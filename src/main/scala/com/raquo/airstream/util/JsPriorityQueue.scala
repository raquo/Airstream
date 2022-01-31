package com.raquo.airstream.util

import com.raquo.ew.JsArray

class JsPriorityQueue[A](getRank: A => Int) {

  private[this] val queue: JsArray[A] = JsArray()

  def enqueue(item: A): Unit = {
    val itemRank = getRank(item)
    var insertAtIndex = 0
    var foundHigherRank = false
    while (
      insertAtIndex < queue.length && !foundHigherRank
    ) {
      if (getRank(queue(insertAtIndex)) > itemRank) {
        foundHigherRank = true
      } else {
        insertAtIndex += 1
      }
    }
    queue.splice(index = insertAtIndex, deleteCount = 0, item) // insert at index
  }

  /** Note: throws exception if there are no items in the queue */
  @inline def dequeue(): A = {
    // We do this dance because JsArray.shift returns `js.undefined` if array is empty
    if (nonEmpty) {
      queue.shift()
    } else {
      throw new Exception("Unable to dequeue an empty JsPriorityQueue")
    }
  }

  def contains(item: A): Boolean = queue.indexOf(item) != -1

  @inline def size: Int = queue.length

  @inline def isEmpty: Boolean = size == 0

  @inline def nonEmpty: Boolean = !isEmpty

  def debugQueue: List[A] = queue.asScalaJsArray.toList
}
