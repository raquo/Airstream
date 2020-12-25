import sbt._

import java.io.File

class SignalZipsGenerator(sourceManaged: File, from: Int, to: Int)
    extends TuplezSourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"SignalZips.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.signal""")
    println()
    println()
    enter(s"""trait SignalZips {""".stripMargin)
    println()
    for (n <- from to to) {
      println(s"""@inline""")
      println(s"""def zip[${tupleType(n)}](""")
      for (i <- 1 to n) {
        println(s"""  s${i}: Signal[T${i}]${if (i < n) ", " else ""}""".stripMargin)
      }
      println(s"""): Signal[(${tupleType(n)})] = new ZipSignal${n}[${tupleType(n)}](${tupleType(n, "s")})""")
      println()
    }
    leave("""}""")
  }

}
