package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{Owned, Owner}

import scala.scalajs.js

class TestableOwner extends Owner {

  def _testPossessions: js.Array[Owned] = possessions

  override def killPossessions(): Unit = {
    super.killPossessions()
  }
}
