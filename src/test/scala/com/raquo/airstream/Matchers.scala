package com.raquo.airstream

import org.scalactic.{Prettifier, source}
import org.scalatest.matchers.should
import org.scalatest.{Assertion, Assertions}

trait Matchers { this: Assertions =>

  val raw: should.Matchers = new should.Matchers {}

  def assertEquals(
    actual: scala.Any,
    expected: scala.Any
  )(
    implicit prettifier: org.scalactic.Prettifier,
    pos: org.scalactic.source.Position
  ): Assertion = {
    assertResult(expected = expected)(actual = actual)
  }

  def assertEquals(
    actual: scala.Any,
    expected: scala.Any,
    clue: scala.Any
  )(
    implicit prettifier: Prettifier,
    pos: source.Position
  ): Assertion = {
    assertResult(expected = expected, clue = clue)(actual = actual)
  }

  implicit def withShouldSyntax[A](value: A): ShouldSyntax[A] = new ShouldSyntax[A](value)

}
