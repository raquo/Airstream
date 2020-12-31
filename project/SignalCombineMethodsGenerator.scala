import sbt._

import java.io.File

class SignalCombineMethodsGenerator(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"SignalCombineMethods.scala"
) {

  override def doGenerate(): Unit = {
    println("package com.raquo.airstream.combine.generated")
    println()
    println("import com.raquo.airstream.signal.Signal")
    println()
    enter(s"private[airstream] trait SignalCombineMethods {")
    println()
    for (n <- from to to) {
      enter(s"""def combine[${tupleType(n)}](""")
      println((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(s"): Signal[(${tupleType(n)})] = {")
      println(s"combineWith(${tupleType(n, "s")})(Tuple${n}.apply[${tupleType(n)}])")
      leave("}")
      println()
      println("/** @param combinator Must not throw! */")
      enter(s"""def combineWith[${tupleType(n)}, Out](""")
      println((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(")(")
      println(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s"): Signal[Out] = {")
      println(s"new CombineSignal${n}(${tupleType(n, "s")}, combinator)")
      leave("}")
      println()
      println("// --")
      println()
    }
    leave("}")
  }

}
