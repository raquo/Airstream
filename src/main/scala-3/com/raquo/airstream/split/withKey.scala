package com.raquo.airstream.split

/** Use this shorthand extractor if you want to name the `key` argument, e.g.:
  * {{{
  * seqSignal.splitSeq(_.id) { case withKey(signal, id) => ... }
  * }}}
  * Or (gasp!) using infix notation:
  * {{{
  * seqSignal.splitSeq(_.id) { case signal withKey id => ... }
  * }}}
  *
  * Note: this implementation is platform-specific because Scala 3.9 requires
  * `infix` declaration while Scala 2.13 does not support it.
  */
object withKey {

  infix def unapply[K, A](signal: KeyedStrictSignal[K, A]): Some[(KeyedStrictSignal[K, A], K)] = {
    Some((signal, signal.key))
  }
}
