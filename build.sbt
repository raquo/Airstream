enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  ("org.scala-js" %%% "scalajs-dom" % "1.1.0").withDottyCompat(scalaVersion.value),
  "app.tulz" %%% "tuplez-full-light" % "0.3.3",
  ("org.scalatest" %%% "scalatest" % "3.2.0" % Test).withDottyCompat(scalaVersion.value)
)

scalaVersion := ScalaVersions.v213

crossScalaVersions := Seq(ScalaVersions.v3RC1, ScalaVersions.v3M3, ScalaVersions.v213, ScalaVersions.v212)

scalacOptions ~= (_.filterNot(Set(
  "-Ywarn-value-discard",
  "-Wvalue-discard"
)))

scalacOptions in Test ~= (_.filterNot { o =>
  o.startsWith("-Ywarn-unused") || o.startsWith("-Wunused")
})

// @TODO[Build] Why does this need " in (Compile, doc)" while other options don't?
scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppress scaladoc "Could not find any member to link for" warnings
)

parallelExecution in Test := false

scalaJSUseMainModuleInitializer := true

scalaJSLinkerConfig in (Compile, fastOptJS) ~= { _.withSourceMap(false) }
