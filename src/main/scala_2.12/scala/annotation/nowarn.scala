package scala.annotation

/** An annotation for local warning suppression added in 2.13.2. Note that this annotation has
  * no functionality when used in Scala 2.11 or 2.12, but allows cross-compiling code that uses
  * `@nowarn`.
  *
  * For documentation on how to use the annotation in 2.13 see
  * https://www.scala-lang.org/api/current/scala/annotation/nowarn.html
  */
class nowarn(value: String = "") extends StaticAnnotation