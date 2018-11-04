package com.raquo.airstream.eventstream

class Tuple2EventStream[A, B](val tuple2Stream: EventStream[(A, B)]) extends AnyVal {

  /** @param project Note: guarded against exceptions */
  def map2[C](project: (A, B) => C): EventStream[C] = {
    new MapEventStream[(A, B), C](
      parent = tuple2Stream,
      project = combinedValue => project(combinedValue._1, combinedValue._2),
      recover = None
    )
  }
}
