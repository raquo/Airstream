import sbt._

import java.io.File

class EventStreamCombinesGenerator(sourceManaged: File, from: Int, to: Int)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "eventstream" / s"EventStreamCombines.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.eventstream""")
    println()
    println()
    enter(s"""private[eventstream] trait EventStreamCombines {""".stripMargin)
    println()
    for (n <- from to to) {
      println(s"""@inline""")
      println(s"""def combine[${tupleType(n)}](""")
      for (i <- 1 to n) {
        println(s"""  s${i}: EventStream[T${i}]${if (i < n) ", " else ""}""".stripMargin)
      }
      println(s"""): EventStream[(${tupleType(n)})] = new CombineEventStream${n}[${tupleType(n)}](${tupleType(n, "s")})""")
      println()
    }
    leave("""}""")
  }

}
