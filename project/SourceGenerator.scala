import java.io.{File, FileOutputStream, PrintStream}

abstract class SourceGenerator(file: File) {

  file.getParentFile.mkdirs()

  private val printStream = new PrintStream(new FileOutputStream(file))

  private var indent      = ""

  protected def enter(s: String = ""): Unit = {
    line(s)
    indent = indent + "  "
  }

  protected def leave(): Unit = {
    indent = indent.substring(0, indent.length - 2)
  }

  protected def leave(s: String): Unit = {
    indent = indent.substring(0, indent.length - 2)
    line(s)
  }

  protected def line(s: String): Unit = {
    printStream.print(indent)
    printStream.println(s)
  }

  protected def line(): Unit = {
    printStream.println()
  }

  protected def done(): Unit = {
    printStream.close()
  }

  protected def tupleTypeRaw(size: Int, prefix: String = "T", suffix: String = ""): Seq[String] =
    (1 to size).map(i => s"${prefix}${i}${suffix}")

  protected def tupleType(size: Int, prefix: String = "T", suffix: String = ""): String =
    tupleTypeRaw(size, prefix, suffix).mkString(", ")

  protected def tupleAccessRaw(size: Int, varName: String): Seq[String] =
    (1 to size).map(i => s"${varName}._${i}")

  protected def tupleAccess(size: Int, varName: String): String =
    tupleAccessRaw(size, varName).mkString(", ")

  final def run: Seq[File] = {
    apply()
    done()
    Seq(file)
  }

  protected def apply(): Unit

}
