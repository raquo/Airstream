import sbt._

import java.io.File

case class GenerateCombineEventStreams(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombineEventStreams.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.combine.CombineEventStreamN")
    line("import com.raquo.airstream.eventstream.EventStream")
    line()
    for (n <- from to to) {
      line("/** @param combinator Must not throw! */")
      enter(s"class CombineEventStream${n}[${tupleType(n)}, Out](")
      for (i <- 1 to n) {
        line(s"parent${i}: EventStream[T${i}],")
      }
      line(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s") extends CombineEventStreamN[Any, Out](")
      line("parents = " + tupleTypeRaw(n, "parent").mkString(" :: ") + " :: Nil,")
      enter("combinator = seq => combinator(")
      for (i <- 1 to n) {
        line(s"seq(${i - 1}).asInstanceOf[T${i}],")
      }
      leave(")")
      leave(")")
      line()
    }
  }

}
