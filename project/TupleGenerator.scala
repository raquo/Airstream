import sbt._

import java.io.File

class TupleGenerator(sourceManaged: File)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "composition" / "Tuple.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.composition""")
    println()
    println("""sealed trait Tuple[T]""")
    println()
    enter("""object Tuple {""")
    println()
    println("""def yes[T]: Tuple[T] = null""")
    println()
    println("""implicit def forNothing[A]: Tuple[Nothing] = null""")
    println("""implicit def forUnit[A]: Tuple[Unit] = null""")
    println("""implicit def forTuple1[A]: Tuple[Tuple1[A]] = null""")
    for (size <- 2 to 22) {
      println(s"""implicit def forTuple${size}[${tupleType(size)}]: Tuple[(${tupleType(size)})] = null""")
    }
    println()
    leave("}")
  }

  def generatePriLow(): Unit = {
    enter("""trait Composition_PriLow extends Composition_PriLowest {""")

    def forSize(size: Int): Unit = {
      val left = tupleType(size - 1)
      enter(s"""implicit def for${size - 1}AndScalar[${left}, R]: Composition[(${left}), R] =""")
      enter(
        s"""Composition[(${left}), R, (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size - 1, "l")}, r)""")
      leave(""")""")
      leave()
    }

    for (size <- 3 to 21) {
      forSize(size)
    }

    println("""implicit def for1and1[L1, R1] = Composition[Tuple1[L1], Tuple1[R1], (L1, R1)]((l, r) => (l._1, r._1))""")

    leave("""}""")
  }

  def generatePriLowButHigher(): Unit = {
    enter("""trait Composition_PriLowButHigher extends Composition_PriLow {""")

    def forSize(size: Int): Unit = {
      val left = tupleType(size - 1)
      enter(s"""implicit def for${size - 1}And1[${left}, R]: Composition[(${left}), Tuple1[R]] =""")
      enter(
        s"""Composition[(${left}), Tuple1[R], (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size - 1, "l")}, r._1)""")
      leave(""")""")
      leave()
    }

    for (size <- 3 to 21) {
      forSize(size)
    }

    leave("""}""")
  }

}
