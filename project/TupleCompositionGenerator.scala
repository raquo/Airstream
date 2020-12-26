import sbt._

import java.io.File

class TupleCompositionGenerator(sourceManaged: File)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "composition" / "TupleComposition.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.composition""")
    println()
    enter("""object TupleComposition {""")
    println()
    println("""def compose[L, R](l: L, r: R)(implicit composition: Composition[L, R]): composition.Composed = composition.compose(l, r)""")
    println()
    leave("}")
    println()
    enter("""abstract class Composition[-A, -B] {""")
    println("""type Composed""")
    println("""val compose: (A, B) => Composed""")
    leave("}")
    println()
    enter("""trait Composition_Pri0 {""")
    println("""implicit def ***[A: NonTuple, B: NonTuple] = Composition[A, B, (A, B)]((_, _))""")
    leave("}")
    println()

    enter("""trait Composition_Pri5 extends Composition_Pri0{""")
    println("""implicit def `T1+scalar`[L, R: NonTuple] = Composition[Tuple1[L], R, (L, R)]((l, r) => (l._1, r))""")
    println("""implicit def `scalar+T1`[L: NonTuple, R] = Composition[L, Tuple1[R], (L, R)]((l, r) => (l, r._1))""")
    leave("}")

    println()

    generatePri7()

    println()

    generatePri10()

    println()

    enter("""trait Composition_Pri20 extends Composition_Pri10 {""")
    println("""implicit def _toA[A] = Composition[Unit, A, A]((_, a) => a)""")
    println("""implicit def Ato_[A] = Composition[A, Unit, A]((a, _) => a)""")
    leave("""}""")
    println()
    enter("""object Composition extends Composition_Pri20 {""")
    println("""implicit def CompositionIsTuple[A, B](c: Composition[A, B]): Tuple[c.Composed] = Tuple.yes""")
    println("""implicit def _to_ = Composition[Unit, Unit, Unit]((_, _) => ())""")
    println("""type Aux[A, B, O] = Composition[A, B] { type Composed = O }""")
    println()
    enter("""def apply[A, B, O](c: (A, B) => O): Aux[A, B, O] =""")
    println("""new Composition[A, B] {""")
    println("""override type Composed = O""")
    println("""val compose: (A, B) => O = c""")
    leave("""}""")
    println()
    leave("""}""")
  }

  def generatePri7(): Unit = {
    enter("""trait Composition_Pri7 extends Composition_Pri5 {""")

    println()

    def forSizeAndScalar(size: Int): Unit = {
      val left = tupleType(size - 1)
      enter(s"""implicit def `T${size - 1}+scalar`[${left}, R: NonTuple] =""")
      enter(
        s"""Composition[(${left}), R, (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size - 1, "l")}, r)""")
      leave(""")""")
      leave()
    }

    def forScalarAndSize(size: Int): Unit = {
      val right = tupleType(size - 1)
      enter(s"""implicit def `scalar+T${size - 1}`[L: NonTuple, ${right}] =""")
      enter(
        s"""Composition[L, (${right}), (L, ${right})]((l, r) =>"""
      )
      println(s"""(l, ${tupleAccess(size - 1, "r")})""")
      leave(""")""")
      leave()
    }

    for (size <- 3 to 22) {
      forSizeAndScalar(size)
      forScalarAndSize(size)
    }

    leave("""}""")
  }

  def generatePri10(): Unit = {
    enter("""trait Composition_Pri10 extends Composition_Pri7 {""")

    println()

    println("""implicit def `T1+T1`[L, R] = Composition[Tuple1[L], Tuple1[R], (L, R)]((l, r) => (l._1, r._1))""")

    def forSizeAnd1(size: Int): Unit = {
      val left = tupleType(size)
      enter(s"""implicit def `T${size}+T1`[${left}, R] =""")
      enter(
        s"""Composition[(${left}), Tuple1[R], (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size, "l")}, r._1)""")
      leave(""")""")
      leave()
    }

    def for1AndSize(size: Int): Unit = {
      val right = tupleType(size)
      enter(s"""implicit def `T1+T${size}`[L, ${right}] =""")
      enter(
        s"""Composition[Tuple1[L], (${right}), (L, ${right})]((l, r) =>"""
      )
      println(s"""(l._1, ${tupleAccess(size, "r")})""")
      leave(""")""")
      leave()
    }

    for (size <- 2 to 21) {
      forSizeAnd1(size)
      for1AndSize(size)
    }

    def forSizes(size1: Int, size2: Int): Unit = {
      val left  = tupleType(size1, "L")
      val right = tupleType(size2, "R")
      enter(s"""implicit def `T${size1}+T${size2}`[${left}, ${right}] =""")
      enter(
        s"""Composition[(${left}), (${right}), (${left}, ${right})]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size1, "l")}, ${tupleAccess(size2, "r")})""")
      leave(""")""")
      leave()
    }

    for (size1 <- 2 to 20) {
      for (size2 <- 2 to 22 - size1) {
        forSizes(size1, size2)
      }
    }

    leave("""}""")
  }

}
