import sbt._

import java.io.File

case class GenerateSampleCombineEventStreams(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"SampleCombineEventStreams.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.combine.SampleCombineEventStreamN")
    line("import com.raquo.airstream.eventstream.EventStream")
    line("import com.raquo.airstream.signal.Signal")
    line()
    for (n <- from to to) {
      line("/** @param combinator Must not throw! */")
      enter(s"class SampleCombineEventStream${n}[T0, ${tupleType(n - 1)}, Out](")
      line("samplingStream: EventStream[T0],")
      for (i <- 1 until n) {
        line(s"sampledSignal${i}: Signal[T${i}],")
      }
      line(s"combinator: (T0, ${tupleType(n - 1)}) => Out")
      leave()
      enter(s") extends SampleCombineEventStreamN[Any, Out](")
      line("samplingStream = samplingStream,")
      line("sampledSignals = " + tupleTypeRaw(n - 1, "sampledSignal").mkString(" :: ") + " :: Nil,")
      enter("combinator = seq => combinator(")
      for (i <- 0 until n) {
        line(s"seq(${i}).asInstanceOf[T${i}],")
      }
      leave(")")
      leave(")")
      line()
    }
    line()
  }

}
