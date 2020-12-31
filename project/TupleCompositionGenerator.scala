import sbt._
import java.io.File

class TupleCompositionGenerator(sourceManaged: File, to: Int, generateConcats: Boolean, generatePrepends: Boolean)
  extends SourceGenerator(
    sourceManaged / "scala" / "app" / "tulz" / "tuplez" / "TupleComposition.scala"
  ) {

  def doGenerate(): Unit = {
    println("""package app.tulz.tuplez""")
    println()
    enter("""object TupleComposition {""")
    println()
    println("""def compose[L, R](l: L, r: R)(implicit composition: Composition[L, R]): composition.Composed = composition.compose(l, r)""")
    println()
    leave("}")
    println()
    enter("""abstract class Composition[-A, -B] {""")
    println("""type Composed""")
    println("""val compose: (A, B) => Composed""")
    leave("}")
    println()
    enter("""trait Composition_Pri0 {""")
    println("""implicit def ***[A, B] = Composition[A, B, (A, B)]((_, _))""")
    leave("}")
    println()

    enter("""trait Composition_Pri5 extends Composition_Pri0{""")
    println("""implicit def `T1+R`[L, R] = Composition[Tuple1[L], R, (L, R)]((l, r) => (l._1, r))""")
    println("""implicit def `L+T1`[L, R] = Composition[L, Tuple1[R], (L, R)]((l, r) => (l, r._1))""")
    leave("}")

    println()

    generatePri7(to, generatePrepends)

    println()

    generatePri10(to, generateConcats)

    println()

    enter("""object Composition extends Composition_Pri10 {""")
    println("""implicit def `unit+unit` = Composition[Unit, Unit, Unit]((_, _) => ())""")
    println("""type Aux[A, B, O] = Composition[A, B] { type Composed = O }""")
    println()
    enter("""def apply[A, B, O](c: (A, B) => O): Aux[A, B, O] =""")
    println("""new Composition[A, B] {""")
    println("""override type Composed = O""")
    println("""val compose: (A, B) => O = c""")
    leave("""}""")
    println()
    leave("""}""")
  }

  def generatePri7(to: Int, generatePrepends: Boolean): Unit = {
    enter("""trait Composition_Pri7 extends Composition_Pri5 {""")

    println()

    def forSizeAndScalar(size: Int): Unit = {
      val left = tupleType(size - 1)
      enter(s"""implicit def `T${size - 1}+scalar`[${left}, R] =""")
      enter(
        s"""Composition[(${left}), R, (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size - 1, "l")}, r)""")
      leave(""")""")
      leave()
    }

    def forScalarAndSize(size: Int): Unit = {
      val right = tupleType(size - 1)
      enter(s"""implicit def `scalar+T${size - 1}`[L, ${right}] =""")
      enter(
        s"""Composition[L, (${right}), (L, ${right})]((l, r) =>"""
      )
      println(s"""(l, ${tupleAccess(size - 1, "r")})""")
      leave(""")""")
      leave()
    }

    for (size <- 3 to to) {
      forSizeAndScalar(size)
      if (generatePrepends) {
        forScalarAndSize(size)
      }
    }

    leave("""}""")
  }

  def generatePri10(to: Int, generateConcats: Boolean): Unit = {
    enter("""trait Composition_Pri10 extends Composition_Pri7 {""")

    println()

    println("""implicit def `T1+T1`[L, R] = Composition[Tuple1[L], Tuple1[R], (L, R)]((l, r) => (l._1, r._1))""")

    println()

    def forSizeAnd1(size: Int): Unit = {
      val left = tupleType(size)
      enter(s"""implicit def `T${size}+T1`[${left}, R] =""")
      enter(
        s"""Composition[(${left}), Tuple1[R], (${left}, R)]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size, "l")}, r._1)""")
      leave(""")""")
      leave()
    }

    def for1AndSize(size: Int): Unit = {
      val right = tupleType(size)
      enter(s"""implicit def `T1+T${size}`[L, ${right}] =""")
      enter(
        s"""Composition[Tuple1[L], (${right}), (L, ${right})]((l, r) =>"""
      )
      println(s"""(l._1, ${tupleAccess(size, "r")})""")
      leave(""")""")
      leave()
    }

    for (size <- 2 until to) {
      forSizeAnd1(size)
      for1AndSize(size)
    }

    def forSizes(size1: Int, size2: Int): Unit = {
      val left  = tupleType(size1, "L")
      val right = tupleType(size2, "R")
      enter(s"""implicit def `T${size1}+T${size2}`[${left}, ${right}] =""")
      enter(
        s"""Composition[(${left}), (${right}), (${left}, ${right})]((l, r) =>"""
      )
      println(s"""(${tupleAccess(size1, "l")}, ${tupleAccess(size2, "r")})""")
      leave(""")""")
      leave()
    }

    if (generateConcats) {
      for (size1 <- 2 to to - 2) {
        for (size2 <- 2 to to - size1) {
          forSizes(size1, size2)
        }
      }
    }

    println("""implicit def `unit+A`[A] = Composition[Unit, A, A]((_, a) => a)""")
    println("""implicit def `A+unit`[A] = Composition[A, Unit, A]((a, _) => a)""")
    println()

    leave("""}""")
  }

}
