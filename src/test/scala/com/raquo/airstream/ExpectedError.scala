package com.raquo.airstream

final case class ExpectedError(msg: String) extends Exception(msg)
