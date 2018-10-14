package com.raquo.airstream.core

import scala.util.Try

class Observation[A](val observable: Observable[A], val value: Try[A])
