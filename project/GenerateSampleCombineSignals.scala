import sbt._

import java.io.File

case class GenerateSampleCombineSignals(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"SampleCombineSignals.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.combine.SampleCombineSignalN")
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("// These are implementations of SampleCombineSignalN used for Signal's `withCurrentValueOf` and `sample` methods")
    line()
    for (n <- from to to) {
      line("/** @param combinator Must not throw! */")
      enter(s"class SampleCombineSignal${n}[T0, ${tupleType(n - 1)}, Out](")
      line("samplingSignal: Signal[T0],")
      for (i <- 1 until n) {
        line(s"sampledSignal${i}: Signal[T${i}],")
      }
      line(s"combinator: (T0, ${tupleType(n - 1)}) => Out")
      leave()
      enter(s") extends SampleCombineSignalN[Any, Out](")
      line("samplingSignal = samplingSignal,")
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
