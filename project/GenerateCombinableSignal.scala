import sbt._

import java.io.File

case class GenerateCombinableSignal(
  sourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  sourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombinableSignal.scala"
) {

  override def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import app.tulz.tuplez.Composition")
    line("import com.raquo.airstream.signal.Signal")
    line()
    line("// These combine / combineWith / withCurrentValueOf / sample methods are implicitly available on all signals")
    line("// For combine / combineWith methods on the Signal companion object, see StaticSignalCombineOps.scala")
    line()
    enter(s"class CombinableSignal[A](val signal: Signal[A]) extends AnyVal {")
    line()
    for (n <- (from - 1) until to) {
      enter(s"def combine[${tupleType(n)}](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(s")(implicit c: Composition[A, (${tupleType(n)})]): Signal[c.Composed] = {")
      line(s"combineWith(${tupleType(n, "s")})((a, ${tupleType(n, "v")}) => c.compose(a, (${tupleType(n, "v")})))")
      leave("}")
      line()
      line("/** @param combinator Must not throw! */")
      enter(s"def combineWith[${tupleType(n)}, Out](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(")(")
      line(s"combinator: (A, ${tupleType(n)}) => Out")
      leave()
      enter(s"): Signal[Out] = {")
      line(s"new CombineSignal${n + 1}(signal, ${tupleType(n, "s")}, combinator)")
      leave("}")
      line()
      enter(s"def withCurrentValueOf[${tupleType(n)}](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(s")(implicit c: Composition[A, (${tupleType(n)})]): Signal[c.Composed] = {")
      line(s"val combinator = (a: A, ${(1 to n).map(i => s"v${i}: T${i}").mkString(", ")}) => c.compose(a, (${tupleType(n, "v")}))")
      line(s"new SampleCombineSignal${n + 1}(signal, ${tupleType(n, "s")}, combinator)")
      leave("}")
      line()
      enter(s"def sample[${tupleType(n)}](")
      line((1 to n).map(i => s"s${i}: Signal[T${i}]").mkString(", "))
      leave()
      enter(s"): Signal[(${tupleType(n)})] = {")
      line(s"new SampleCombineSignal${n + 1}[A, ${tupleType(n)}, (${tupleType(n)})](signal, ${tupleType(n, "s")}, (_, ${tupleType(n, "v")}) => (${tupleType(n, "v")}))")
      leave("}")
      line()
      line("// --")
      line()
    }
    leave("}")
  }

}
