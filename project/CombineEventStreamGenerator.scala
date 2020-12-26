import sbt._

import java.io.File

class CombineEventStreamGenerator(sourceManaged: File, n: Int)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "eventstream" / s"CombineEventStream${n}.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.eventstream""")
    println()
    println(s"""class CombineEventStream${n}[${tupleType(n)}](""")
    for (i <- 1 to n) {
      println(s"""  protected[this] val parent${i}: EventStream[T${i}]${if (i < n) "," else ""}""".stripMargin)
    }
    println(s") extends CombineNEventStream[Any, (${tupleType(n)})](")
    println("  Seq(" + tupleType(n, "parent") + ")")
    enter(s"){")
    println()

    println(s"""def toOut(seq: Seq[Any]): (${tupleType(n)}) = """)
    println(s"""  (${(0 until n).map(i => s"""seq(${i})""").mkString(", ")}).asInstanceOf[(${tupleType(n)})]""")

    println()

    leave("""}""")
  }

}
