import sbt._

import java.io.File

class CompositionTestGenerator(sourceManaged: File)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "composition" / "CompositionTests.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.composition""")
    println()
    println("""import com.raquo.airstream.UnitSpec""".stripMargin)
    println()
    enter("""class CompositionTests extends UnitSpec {""")
    println()

    println("""private val unit: Unit = (): Unit""")
    println()

    enter("""it("Unit plus Unit") {""")
    println("""TupleComposition.compose(unit, unit) should ===(unit)""")
    leave("}")
    println()

    def tupleValue(size: Int, offset: Int): String = {
      if (size == 1) {
        s"Tuple1(${offset + 1})"
      } else {
        s"(${(offset + 1 to offset + size).mkString(", ")})"
      }
    }

    for (size1 <- 1 to 21) {
      enter(s"""it("${size1}-tuple plus Unit") {""")
      println(s"""val tuple = ${tupleValue(size1, 100)}""")
      println(s"""TupleComposition.compose(tuple, (): Unit) should ===(tuple)""")
      println(s"""TupleComposition.compose((): Unit, tuple) should ===(tuple)""")
      leave(s"""}""")
      println()
    }

    for (size1 <- 1 to 21) {
      enter(s"""it("${size1}-tuple plus scalar") {""")
      println(s"""val tuple = ${tupleValue(size1, 100)}""")
      println(s"""val expected = (${(101 until 101 + size1).mkString(", ")}, 201)""")
      println(s"""TupleComposition.compose(tuple, 201) should ===(expected)""")
      leave(s"""}""")
      println()
    }

    for (size1 <- 1 to 21) {
      enter(s"""it("scalar plus ${size1}-tuple") {""")
      println(s"""val tuple = ${tupleValue(size1, 100)}""")
      println(s"""val expected = (201, ${(101 until 101 + size1).mkString(", ")})""")
      println(s"""TupleComposition.compose(201, tuple) should ===(expected)""")
      leave(s"""}""")
      println()
    }

    for (size1 <- 1 to 21) {
      for (size2 <- 1 to 22 - size1) {
        enter(s"""it("${size1}-tuple plus ${size2}-tuple") {""".stripMargin)
        println(s"""val tuple1 = ${tupleValue(size1, 100)}""")
        println(s"""val tuple2 = ${tupleValue(size2, 200)}""")
        println(s"""val expected = (${((101 until 101 + size1) ++ (201 until 201 + size2)).mkString(", ")})""")
        println(s"""TupleComposition.compose(tuple1, tuple2) should ===(expected)""")
        leave(s"""}""")
        println()
      }
    }

    println()

    leave("""}""")
  }

}
