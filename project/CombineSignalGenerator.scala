import sbt._

import java.io.File

class CombineSignalGenerator(sourceDir: File, n: Int) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombineSignal${n}.scala"
) {

  override def doGenerate(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.combine.CombineSignalN")
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("/** @param combinator Must not throw!*/")
    enter(s"""class CombineSignal${n}[${tupleType(n)}, Out](""")
    for (i <- 1 to n) {
      line(s"protected[this] val parent${i}: Signal[T${i}],")
    }
    line(s"combinator: (${tupleType(n)}) => Out")
    leave()
    enter(s") extends CombineSignalN[Any, Out](")
    line("parents = List(" + tupleType(n, "parent") + "),")
    enter("combinator = seq => combinator(")
    for (i <- 1 to n) {
      line(s"seq(${i - 1}).asInstanceOf[T${i}],")
    }
    leave(")")
    leave(")")
  }

}
