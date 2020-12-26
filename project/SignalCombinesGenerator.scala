import sbt._

import java.io.File

class SignalCombinesGenerator(sourceManaged: File, from: Int, to: Int)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"SignalCombines.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.signal""")
    println()
    println()
    enter(s"""private[signal] trait SignalCombines {""".stripMargin)
    println()
    for (n <- from to to) {
      println(s"""@inline""")
      println(s"""def combine[${tupleType(n)}](""")
      for (i <- 1 to n) {
        println(s"""  s${i}: Signal[T${i}]${if (i < n) ", " else ""}""".stripMargin)
      }
      println(s"""): Signal[(${tupleType(n)})] = new CombineSignal${n}[${tupleType(n)}](${tupleType(n, "s")})""")
      println()
    }
    leave("""}""")
  }

}
