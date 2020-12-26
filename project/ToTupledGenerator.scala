import sbt._

import java.io.File

class ToTupledGenerator(sourceManaged: File)
    extends SourceGenerator(
      sourceManaged / "com" / "raquo" / "airstream" / "composition" / "ToTupled.scala"
    ) {

  def doGenerate(): Unit = {
    println("""package com.raquo.airstream.composition""")
    println()
    enter("""object ToTupled {""")
    println("""def apply[In, L](f: In): L => O""")
    leave("""}""")
    println()
    println("""trait ApplyConverters[O] extends ApplyConverterInstances[O]""")
  }

}
