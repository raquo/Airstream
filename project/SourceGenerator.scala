import sbt._

import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

abstract class SourceGenerator(file: File) {

  file.getParentFile.mkdirs()
  private val printStream = new PrintStream(new FileOutputStream(file))
  private var indent      = ""

  protected def enter(s: String = ""): Unit = {
    println(s)
    indent = indent + "  "
  }

  protected def leave(): Unit = {
    indent = indent.substring(0, indent.length - 2)
  }

  protected def leave(s: String): Unit = {
    indent = indent.substring(0, indent.length - 2)
    println(s)
  }

  protected def println(s: String): Unit = {
    printStream.print(indent)
    printStream.println(s)
  }

  protected def println(): Unit = {
    printStream.println()
  }

  protected def done(): Unit = {
    printStream.close()
  }

  protected def tupleTypeRaw(size: Int, prefix: String = "T", suffix: String = ""): Seq[String] =
    (1 to size).map(i => s"${prefix}${i}${suffix}")

  protected def tupleType(size: Int, prefix: String = "T", suffix: String = ""): String =
    tupleTypeRaw(size, prefix, suffix).mkString(", ")

  final def generate(): Seq[File] = {
    doGenerate()
    done()
    Seq(file)
  }

  protected def doGenerate(): Unit

}
