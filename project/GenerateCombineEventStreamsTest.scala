import sbt._

import java.io.File

case class GenerateCombineEventStreamsTest(
  testSourceDir: File,
  from: Int,
  to: Int
) extends SourceGenerator(
  testSourceDir / "scala" / "com" / "raquo" / "airstream" / "combine" / "generated" / s"CombineEventStreamsSpec.scala"
) {

  def apply(): Unit = {
    line("package com.raquo.airstream.combine.generated")
    line()
    line("import com.raquo.airstream.UnitSpec")
    line("import com.raquo.airstream.core.Observer")
    line("import com.raquo.airstream.eventbus.EventBus")
    line("import com.raquo.airstream.eventstream.EventStream")
    line("import com.raquo.airstream.fixtures.TestableOwner")
    line()
    line("import scala.collection.mutable")
    line()
    enter(s"class CombineEventStreamsSpec extends UnitSpec {")
    line()
    for (i <- 1 to to) {
      line(s"case class T${i}(v: Int)")
    }
    line()
    for (n <- from to to) {
      enter(s"""it("CombineEventStream${n} works") {""")
      line()
      line("implicit val testOwner: TestableOwner = new TestableOwner")
      line()
      for (i <- 1 to n) {
        line(s"val bus${i} = new EventBus[T${i}]()")
      }
      line()
      line(s"val combinedStream = EventStream.combine(${tupleType(n, "bus", ".events")})")
      line()
      line(s"val effects = mutable.Buffer[(${tupleType(n)})]()")
      line()
      line(s"val observer = Observer[(${tupleType(n)})](effects += _)")
      line()
      line("// --")
      line()
      line("effects.toList shouldBe empty")
      line()
      line("// --")
      line("val subscription = combinedStream.addObserver(observer)")
      line("effects.toList shouldBe empty")
      line()
      line("// --")
      line()

      for (i <- 1 to n) {
        line(s"bus${i}.writer.onNext(T${i}(0))")
        if (i < n) {
          line("effects.toList shouldBe empty")
        } else {
          enter("effects.toList shouldEqual List(")
          line(s"(${(1 to n).map(i => s"T${i}(0)").mkString(", ")})")
          leave(")")
        }
        line()
      }

      line("// --")

      enter("for (iteration <- 1 to 10) {")
      line("effects.clear()")
      for (i <- 1 to n) {
        line(s"bus${i}.writer.onNext(T${i}(iteration))")
      }
      enter("effects.toList should ===(")
      enter("List(")
      for (i <- 1 to n) {
        line(s"(${(1 to n).map(j => s"T${j}(iteration${if (j <= i) "" else " - 1"})").mkString(", ")})${if (i < n) "," else ""}")
      }
      leave(")")
      leave(")")
      leave("}")
      line()
      line("subscription.kill()")

      leave("}")
      line()
    }
    line()
    leave("}")
  }

}
