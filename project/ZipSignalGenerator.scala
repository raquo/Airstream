import sbt._

import java.io.File

class ZipSignalGenerator(sourceManaged: File, n: Int)
    extends TuplezSourceGenerator(
      sourceManaged / "scala" / "com" / "raquo" / "airstream" / "signal" / s"ZipSignal${n}.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.signal""")
    println()
    println("""import com.raquo.airstream.core.AirstreamError.CombinedError""")
    println("""import com.raquo.airstream.features.{ CombineObservable, InternalParentObserver }""".stripMargin)
    println("""import scala.util.{ Failure, Success, Try }""")
    println()
    println(s"""class ZipSignal${n}[${tupleType(n)}](""")
    for (i <- 1 to n) {
      println(s"""  protected[this] val parent${i}: Signal[T${i}]${if (i < n) "," else ""}""".stripMargin)
    }
    enter(s") extends Signal[(${tupleType(n)})] with CombineObservable[(${tupleType(n)})] {")
    println()
    enter("""override protected[airstream] val topoRank: Int = (""")
    println((1 to n).map(i => s"parent${i}.topoRank").mkString(" max "))
    leave(""") + 1""")
    println()
    enter("""parentObservers.push(""")
    for (i <- 1 to n) {
      enter(s"""InternalParentObserver.fromTry[T${i}](parent${i}, (nextValue, transaction) => {""")

      val getValues =
        (1 to n).map(j =>
          if (j == i) {
            "nextValue"
          } else {
            s"""parent${j}.tryNow()"""
          }
        ).mkString(", ")

      println(
        s"""internalObserver.onTry(guarded(${getValues}), transaction)"""

      )

      if (i < n) {
        leave("""}),""")
      } else {
        leave("""})""")
      }

    }
    leave(""")""")

    println()

    println("""private def guarded(""")
    for (i <- 1 to n) {
      println(s"""  t${i}: Try[T${i}]${if (i < n) "," else ""}""")
    }
    enter(""") = {""")

    enter(s"""if (Seq(${tupleType(n, "t")}).forall(_.isSuccess)) {""")
    println(s"""Success((${tupleType(n, "t", ".get")}))""")
    leave()
    enter("""} else {""")
    println(s"""Failure(CombinedError(Seq(${tupleType(n, "t")}).map(_.failed.toOption)))""")
    leave("}")
    leave("}")

    println()

    enter(s"""override protected[this] def initialValue: Try[(${tupleType(n)})] =""")
    println(s"""guarded(${tupleType(n, "parent", ".tryNow()")})""")
    leave()
    println()

    leave("""}""")
  }

}
