package com.raquo.airstream

import org.scalactic.{Prettifier, source}
import org.scalatest.Assertion
import org.scalatest.enablers.Emptiness
import org.scalatest.matchers.should

// I don't want to use the full variety of ScalaTest "should" matchers.
// Those that we actually need should be defined as simple methods here.

class ShouldSyntax[A](val actual: A) extends AnyVal {

  def shouldBe(
    expected: scala.Any
  )(
    implicit pos: source.Position,
    prettifier: Prettifier
  ): Assertion = {
    ShouldSyntax.shouldBe(actual, expected)(pos, prettifier)
  }

  def shouldBeEmpty(
    implicit pos: source.Position,
    prettifier: Prettifier,
    emptiness: Emptiness[A]
  ): Assertion = {
    ShouldSyntax.shouldBeEmpty(actual)(pos, prettifier, emptiness)
  }

  def shouldNotBe(
    expected: scala.Any
  )(
    implicit pos: source.Position,
    prettifier: Prettifier
  ): Assertion = {
    ShouldSyntax.shouldNotBe(actual, expected)(pos, prettifier)
  }
}

object ShouldSyntax extends should.Matchers {

  // #Note ScalaTest generates different code for Scala 2 and Scala 3.
  //  In particular, it does not emit convertToAnyShouldWrapper in Scala 3.
  //  I don't care enough to figure out what or why.
  //  If you do, see SKIP-DOTTY-START and SKIP-DOTTY-STOP in ScalaTest code and go from there.

  def shouldBe[A](
    actual: A,
    expected: scala.Any
  )(
    implicit pos: source.Position,
    prettifier: Prettifier
  ): Assertion = {
    actual shouldBe expected
  }

  def shouldBeEmpty[A](
    actual: A
  )(
    implicit pos: source.Position,
    prettifier: Prettifier,
    emptiness: Emptiness[A]
  ): Assertion = {
    actual shouldBe empty
  }

  def shouldNotBe[A](
    actual: A,
    expected: scala.Any
  )(
    implicit pos: source.Position,
    prettifier: Prettifier
  ): Assertion = {
    actual shouldNot be(expected)
  }
}
