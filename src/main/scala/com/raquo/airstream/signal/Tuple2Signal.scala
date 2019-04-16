package com.raquo.airstream.signal

// @TODO[API] I think this can be a Tuple2Observable, or maybe even an actual native method with evidence

class Tuple2Signal[A, B](val tuple2Signal: Signal[(A, B)]) extends AnyVal {

  def map2[C](project: (A, B) => C): Signal[C] = {
    new MapSignal[(A, B), C](
      parent = tuple2Signal,
      combinedValue => project(combinedValue._1, combinedValue._2),
      recover = None
    )
  }
}

