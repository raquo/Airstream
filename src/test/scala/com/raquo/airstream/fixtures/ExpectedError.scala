package com.raquo.airstream.fixtures

final case class ExpectedError(msg: String) extends Exception(msg)
