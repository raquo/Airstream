import sbt._

import java.io.File

class ZipSignalTestGenerator(sourceManaged: File, from: Int, to: Int)
    extends TuplezSourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"ZipSignalSpec.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.signal""")
    println()
    println("""import com.raquo.airstream.UnitSpec""")
    println("""import com.raquo.airstream.core.Observer""")
    println("""import com.raquo.airstream.fixtures.TestableOwner""")
    println("""import scala.collection.mutable""")
    println()
    enter(s"""class ZipSignalSpec extends UnitSpec {""")
    println()
    for (i <- 1 to to) {
      println(s"""case class T${i}(v: Int) { def inc: T${i} = T${i}(v+1) }""")
    }
    println()
    for (n <- from to to) {
      enter(s"""it("ZipSignal${n} should work as expected") {""")
      println()
      println("""implicit val testOwner: TestableOwner = new TestableOwner""")
      println()
      for (i <- 1 to n) {
        println(s"""val var${i} = Var(T${i}(1))""")
      }
      println()
      println(s"""val combinedSignal = Signal.zip(${tupleType(n, "var", ".signal")})""")
      println()
      println(s"""val effects = mutable.Buffer[(${tupleType(n)})]()""")
      println()
      println(s"""val observer = Observer[(${tupleType(n)})](effects += _)""")
      println()
      println("""// --""")
      println()
      println("""effects.toList shouldBe empty""")
      println()
      println("""// --""")
      println()
      println("""val subscription = combinedSignal.addObserver(observer)""")
      println()
      println("""// --""")
      println()
      enter("""effects.toList shouldEqual List(""")
      println(s"(${(1 to n).map(i => s"T${i}(1)").mkString(", ")})")
      leave(")")
      println()
      println("""// --""")
      println()

      enter("""for (iteration <- 0 until 10) {""")
      println("""effects.clear()""")
      for (i <- 1 to n) {
        println(s"""var${i}.update(_.inc)""")
      }
      enter("""effects.toList shouldEqual""")
      enter("""List(""")
      for (i <- 1 to n) {
        println(s"""(${(1 to n).map(j => s"""T${j}(1 + iteration${if (j <= i) " + 1" else ""})""").mkString(", ")})${if (i < n) "," else ""}""")
      }
      leave(""")""")
      leave()
      leave("""}""")
      println()
      println("""subscription.kill()""")

      leave("""}""")
      println()
    }
    println()
    leave("""}""")
  }

}
