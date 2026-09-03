addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("com.raquo" % "sbt-buildkit" % "0.2.0-M1")

addSbtPlugin("com.raquo" % "sbt-buildkit-scalajs" % "0.2.0-M1")

addSbtPlugin("com.raquo" % "sbt-buildkit-dynver" % "0.2.0-M1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1"
