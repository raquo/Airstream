import sbt._

import java.io.File

class EventStreamCombineMethodsGenerator(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"EventStreamCombineMethods.scala"
) {

  override def doGenerate(): Unit = {
    println("package com.raquo.airstream.combine.generated")
    println()
    println("import com.raquo.airstream.eventstream.EventStream")
    println()
    enter(s"private[airstream] trait EventStreamCombineMethods {")
    println()
    for (n <- from to to) {
      enter(s"""def combine[${tupleType(n)}](""")
      println((1 to n).map(i => s"s${i}: EventStream[T${i}]").mkString(", "))
      leave()
      enter(s"): EventStream[(${tupleType(n)})] = {")
      println(s"combineWith(${tupleType(n, "s")})(Tuple${n}.apply[${tupleType(n)}])")
      leave("}")
      println()
      println("/** @param combinator Must not throw! */")
      enter(s"""def combineWith[${tupleType(n)}, Out](""")
      println((1 to n).map(i => s"s${i}: EventStream[T${i}]").mkString(", "))
      leave()
      enter(")(")
      println(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s"): EventStream[Out] = {")
      println(s"new CombineEventStream${n}(${tupleType(n, "s")}, combinator)")
      leave("}")
      println()
      println("// --")
      println()
    }
    leave("}")
  }

}
