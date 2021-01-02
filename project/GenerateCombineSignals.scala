import sbt._

import java.io.File

case class GenerateCombineSignals(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombineSignals.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.combine.CombineSignalN")
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("// These are implementations of CombineSignalN used for Signal's `combine` and `combineWith` methods")
    line()
    for (n <- from to to) {
      line("/** @param combinator Must not throw! */")
      enter(s"class CombineSignal${n}[${tupleType(n)}, Out](")
      for (i <- 1 to n) {
        line(s"parent${i}: Signal[T${i}],")
      }
      line(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s") extends CombineSignalN[Any, Out](")
      line("parents = " + tupleTypeRaw(n, "parent").mkString(" :: ") + " :: Nil,")
      enter("combinator = seq => combinator(")
      for (i <- 1 to n) {
        line(s"seq(${i - 1}).asInstanceOf[T${i}],")
      }
      leave(")")
      leave(")")
      line()
    }
    line()
  }

}
