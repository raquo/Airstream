package com.raquo.airstream.split

/** Use this shorthand extractor if you want to name the `key` argument, e.g.:
  * {{{
  * foosVar.splitSeq(_.id) { case varWithKey(fooVar, id) => ... }
  * }}}
  * Or (gasp!) using infix notation:
  * {{{
  * foosVar.splitSeq(_.id) { case fooVar varWithKey id => ... }
  * }}}
  *
  * Note: this implementation is platform-specific because Scala 3.9 requires
  * `infix` declaration while Scala 2.13 does not support it.
  */
object varWithKey {

  def unapply[K, A](v: KeyedDerivedVar[K, ?, A]): Some[(KeyedDerivedVar[K, ?, A], K)] = {
    Some((v, v.key))
  }
}
