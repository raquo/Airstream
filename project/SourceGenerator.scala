import java.io.{File, FileOutputStream, PrintStream}

abstract class SourceGenerator(outputFile: File) {

  private lazy val printStream = {
    outputFile.getParentFile.mkdirs()
    new PrintStream(new FileOutputStream(outputFile))
  }

  private val indentStep = "  "

  private var currentIndent = ""

  /** Override this to implement the generator. */
  protected def apply(): Unit

  /** Call this to run the generator. */
  final def run: List[File] = {
    apply()
    printStream.close()
    List(outputFile)
  }

  protected def enter(prefix: String, suffix: String = "")(inside: => Unit): Unit = {
    line(prefix)
    val originalIndent = currentIndent
    currentIndent = currentIndent + indentStep
    inside
    currentIndent = originalIndent
    if (suffix.nonEmpty) {
      line(suffix)
    }
  }

  protected def line(str: String): Unit = {
    printStream.print(currentIndent)
    printStream.println(str)
  }

  protected def line(): Unit = {
    printStream.println()
  }

  protected def tupleType(size: Int, prefix: String = "T", suffix: String = "", separator: String = ", "): String =
    tupleTypeRaw(size, prefix, suffix).mkString(separator)

  protected def tupleAccess(size: Int, varName: String): String = {
    tupleAccessRaw(size, varName).mkString(", ")
  }

  private def tupleTypeRaw(size: Int, prefix: String = "T", suffix: String = ""): Seq[String] = {
    (1 to size).map(i => s"${prefix}${i}${suffix}")
  }

  private def tupleAccessRaw(size: Int, varName: String): Seq[String] = {
    (1 to size).map(i => s"${varName}._${i}")
  }

}
