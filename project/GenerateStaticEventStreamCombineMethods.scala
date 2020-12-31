import sbt._

import java.io.File

case class GenerateStaticEventStreamCombineMethods(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"StaticEventStreamCombineMethods.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.eventstream.EventStream")
    line()
    enter(s"private[airstream] trait StaticEventStreamCombineMethods {")
    line()
    for (n <- from to to) {
      enter(s"def combine[${tupleType(n)}](")
      line((1 to n).map(i => s"s${i}: EventStream[T${i}]").mkString(", "))
      leave()
      enter(s"): EventStream[(${tupleType(n)})] = {")
      line(s"combineWith(${tupleType(n, "s")})(Tuple${n}.apply[${tupleType(n)}])")
      leave("}")
      line()
      line("/** @param combinator Must not throw! */")
      enter(s"def combineWith[${tupleType(n)}, Out](")
      line((1 to n).map(i => s"s${i}: EventStream[T${i}]").mkString(", "))
      leave()
      enter(")(")
      line(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s"): EventStream[Out] = {")
      line(s"new CombineEventStream${n}(${tupleType(n, "s")}, combinator)")
      leave("}")
      line()
      line("// --")
      line()
    }
    leave("}")
  }

}
