enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "1.1.0",
  "app.tulz" %%% "tuplez-full-light" % "0.3.0",
  "org.scalatest" %%% "scalatest" % "3.2.0" % Test
)

val filterScalacOptions = { options: Seq[String] =>
  options.filterNot(Set(
    "-Ywarn-value-discard",
    "-Wvalue-discard"
  ))
}

scalaVersion := "2.13.4"

crossScalaVersions := Seq("2.12.12", "2.13.4")

scalacOptions ~= filterScalacOptions

// @TODO[Build] Why does this need " in (Compile, doc)" while other options don't?
scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppress scaladoc "Could not find any member to link for" warnings
)

val generateTupleCombinatorsFrom = 2
val generateExtraTupleCombinatorsFrom = 3
val generateTupleCombinatorsTo = 10

Compile / sourceGenerators += Def.task {
  Seq.concat(
    (generateTupleCombinatorsFrom to generateTupleCombinatorsTo).flatMap(n =>
      new CombineEventStreamGenerator((Compile / sourceDirectory).value, n).generate()
    ),
    (generateTupleCombinatorsFrom to generateTupleCombinatorsTo).flatMap(n =>
      new CombineSignalGenerator((Compile / sourceDirectory).value, n).generate()
    ),
    new EventStreamCombineMethodsGenerator(
      (Compile / sourceDirectory).value,
      from = generateExtraTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).generate(),
    new SignalCombineMethodsGenerator(
      (Compile / sourceDirectory).value,
      from = generateExtraTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).generate()
  )
}.taskValue

Test / sourceGenerators += Def.task {
  Seq.concat(
    new CombineSignalTestGenerator(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).generate(),
    new CombineEventStreamTestGenerator(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).generate()
  )
}.taskValue

mappings in (Compile, packageSrc) ++= {
  val base  = (sourceManaged in Compile).value
  val files = (managedSources in Compile).value
  files.map { f =>
    (f, f.relativeTo(base / "scala").get.getPath)
  }
}

version in installJsdom := "16.4.0"

useYarn := true

requireJsDomEnv in Test := true

parallelExecution in Test := false

scalaJSUseMainModuleInitializer := true

scalaJSLinkerConfig in (Compile, fastOptJS) ~= { _.withSourceMap(false) }
