import sbt._

import java.io.File

case class GenerateStaticSignalCombineOps(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"StaticSignalCombineOps.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("// These combine and combineWith methods are available on the Signal companion object")
    line("// For instance methods of the same name, see CombinableSignal.scala")
    line()
    enter(s"object StaticSignalCombineOps {")
    line()
    for (n <- from to to) {
      enter(s"def combine[${tupleType(n)}](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(s"): Signal[(${tupleType(n)})] = {")
      line(s"combineWith(${tupleType(n, "s")})(Tuple${n}.apply[${tupleType(n)}])")
      leave("}")
      line()
      line("/** @param combinator Must not throw! */")
      enter(s"def combineWith[${tupleType(n)}, Out](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(")(")
      line(s"combinator: (${tupleType(n)}) => Out")
      leave()
      enter(s"): Signal[Out] = {")
      line(s"new CombineSignal${n}(${tupleType(n, "s")}, combinator)")
      leave("}")
      line()
      line("// --")
      line()
    }
    leave("}")
  }

}
