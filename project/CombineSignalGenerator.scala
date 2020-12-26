import sbt._

import java.io.File

class CombineSignalGenerator(sourceManaged: File, n: Int)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"CombineSignal${n}.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.signal""")
    println()
    println(s"""class CombineSignal${n}[${tupleType(n)}](""")
    for (i <- 1 to n) {
      println(s"""  protected[this] val parent${i}: Signal[T${i}]${if (i < n) "," else ""}""".stripMargin)
    }
    println(s") extends CombineNSignal[Any, (${tupleType(n)})](")
    println("  Seq(" + tupleType(n, "parent") + ")")
    enter(s"){")
    println()

    println(s"""def toOut(seq: Seq[Any]): (${tupleType(n)}) = """)
    println(s"""  (${(0 until n).map(i => s"""seq(${i})""").mkString(", ")}).asInstanceOf[(${tupleType(n)})]""")

    println()

    leave("""}""")
  }

}
