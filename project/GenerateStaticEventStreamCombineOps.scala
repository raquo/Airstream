import sbt._

import java.io.File

case class GenerateStaticEventStreamCombineOps(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"StaticEventStreamCombineOps.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.eventstream.EventStream")
    line()
    line("// These combine and combineWith methods are available on the EventStream companion object")
    line("// For instance methods of the same name, see CombinableEventStream.scala")
    line()
    enter(s"object StaticEventStreamCombineOps {")
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
