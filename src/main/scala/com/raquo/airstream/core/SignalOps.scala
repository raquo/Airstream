package com.raquo.airstream.core

import scala.util.Try

trait SignalOps[+A] {

  protected[airstream] def tryNow(): Try[A]

  protected[airstream] def now(): A

}
