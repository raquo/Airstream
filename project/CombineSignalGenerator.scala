import sbt._

import java.io.File

class CombineSignalGenerator(sourceDir: File, n: Int) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombineSignal${n}.scala"
) {

  override def doGenerate(): Unit = {
    println("package com.raquo.airstream.combine.generated")
    println()
    println("import com.raquo.airstream.combine.CombineSignalN")
    println("import com.raquo.airstream.signal.Signal")
    println()
    println("/** @param combinator Must not throw!*/")
    enter(s"""class CombineSignal${n}[${tupleType(n)}, Out](""")
    for (i <- 1 to n) {
      println(s"protected[this] val parent${i}: Signal[T${i}],")
    }
    println(s"combinator: (${tupleType(n)}) => Out")
    leave()
    enter(s") extends CombineSignalN[Any, Out](")
    println("parents = List(" + tupleType(n, "parent") + "),")
    enter("combinator = seq => combinator(")
    for (i <- 1 to n) {
      println(s"seq(${i - 1}).asInstanceOf[T${i}],")
    }
    leave(")")
    leave(")")
  }

}
