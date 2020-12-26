import sbt._

import java.io.File

class CombineEventStreamTestGenerator(sourceManaged: File, from: Int, to: Int)
    extends SourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"CombineEventStreamSpec.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.eventstream""")
    println()
    println("""import com.raquo.airstream.UnitSpec""")
    println("""import com.raquo.airstream.eventbus.EventBus""")
    println("""import com.raquo.airstream.core.Observer""")
    println("""import com.raquo.airstream.fixtures.TestableOwner""")
    println("""import scala.collection.mutable""")
    println()
    enter(s"""class CombineEventStreamSpec extends UnitSpec {""")
    println()
    for (i <- 1 to to) {
      println(s"""case class T${i}(v: Int)""")
    }
    println()
    for (n <- from to to) {
      enter(s"""it("CombineSignal${n} should work as expected") {""")
      println()
      println("""implicit val testOwner: TestableOwner = new TestableOwner""")
      println()
      for (i <- 1 to n) {
        println(s"""val bus${i} = new EventBus[T${i}]()""")
      }
      println()
      println(s"""val combinedStream = EventStream.combine(${tupleType(n, "bus", ".events")})""")
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
      println("""val subscription = combinedStream.addObserver(observer)""")
      println("""effects.toList shouldBe empty""")
      println()
      println("""// --""")
      println()

      for (i <- 1 to n) {
        println(s"""bus${i}.writer.onNext(T${i}(0))""".stripMargin)
        if (i < n) {
          println("""effects.toList shouldBe empty""")
        } else {
          enter("""effects.toList shouldEqual List(""")
          println(s"(${(1 to n).map(i => s"T${i}(0)").mkString(", ")})")
          leave(")")
        }
        println()
      }

      println("""// --""")

      enter("""for (iteration <- 1 to 10) {""")
      println("""effects.clear()""")
      for (i <- 1 to n) {
        println(s"""bus${i}.writer.onNext(T${i}(iteration))""")
      }
      enter("""effects.toList should ===(""")
      enter("""List(""")
      for (i <- 1 to n) {
        println(s"""(${(1 to n).map(j => s"""T${j}(iteration${if (j <= i) "" else " - 1"})""").mkString(", ")})${if (i < n) "," else ""}""")
      }
      leave(""")""")
      leave(""")""")
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
