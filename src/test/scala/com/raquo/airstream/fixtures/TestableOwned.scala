package com.raquo.airstream.fixtures

import com.raquo.airstream.ownership.{Owned, Owner}

class TestableOwned(override val owner: Owner) extends Owned {

  var killCount = 0

  init()

  override def kill(): Unit = super.kill()

  override def onKilled(): Unit = {
    killCount += 1
  }
}
