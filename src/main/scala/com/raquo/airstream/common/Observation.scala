package com.raquo.airstream.common

import com.raquo.airstream.core.Observable

import scala.util.Try

class Observation[A](val observable: Observable[A], val value: Try[A])
