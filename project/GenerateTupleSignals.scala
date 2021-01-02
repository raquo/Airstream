import sbt._

import java.io.File

case class GenerateTupleSignals(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "basic" / "generated" / s"TupleSignals.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.basic.generated")
    line()
    line("import com.raquo.airstream.basic.MapSignal")
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("// These mapN helpers are implicitly available on signals of tuples")
    line()
    for (n <- from to to) {
      enter(s"class TupleSignal${n}[${tupleType(n)}](val signal: Signal[(${tupleType(n)})]) extends AnyVal {")
      line()
      enter(s"def mapN[Out](project: (${tupleType(n)}) => Out): Signal[Out] = {")
      enter(s"new MapSignal[(${tupleType(n)}), Out](")
      line("parent = signal,")
      line(s"project = v => project(${tupleType(n, "v._")}),")
      line(s"recover = None")
      leave(")")
      leave("}")
      leave("}")
      line()
      line("// --")
      line()
    }
  }
}
