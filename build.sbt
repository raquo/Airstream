enablePlugins(ScalaJSPlugin)

//enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "app.tulz" %%% "tuplez-full-light" % "0.3.3-SNAPSHOT"
)

libraryDependencies ++=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0",
      "org.scalatest" %%% "scalatest" % "3.2.0" % Test
    )
    case Some((3, _)) => Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.2.0.3M3-SNAPSHOT"
    )
    case _            => Seq()
  })

val filterScalacOptions = { options: Seq[String] =>
  options.filterNot(Set(
    "-Ywarn-value-discard",
    "-Wvalue-discard"
  ))
}

val filterTestScalacOptions = { options: Seq[String] =>
  options.filterNot { o =>
    o.startsWith("-Ywarn-unused") || o.startsWith("-Wunused")
  }
}

val scala213Version = "2.13.4"
val scala212Version = "2.12.12"
val scala3Version = "3.0.0-RC1-bin-20210113-8345078-NIGHTLY"

scalaVersion := scala3Version

crossScalaVersions := Seq(scala3Version, scala212Version, scala213Version)

scalacOptions ~= filterScalacOptions

scalacOptions in Test ~= filterTestScalacOptions

// @TODO[Build] Why does this need " in (Compile, doc)" while other options don't?
scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppress scaladoc "Could not find any member to link for" warnings
)

val generateTupleCombinatorsFrom = 2
val generateTupleCombinatorsTo = 9

Compile / sourceGenerators += Def.task {
  Seq.concat(
    GenerateCombineEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateSampleCombineEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateSampleCombineSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableEventStream(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableSignal(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateStaticEventStreamCombineOps(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateStaticSignalCombineOps(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue

Test / sourceGenerators += Def.task {
  Seq.concat(
    GenerateCombineSignalsTest(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineEventStreamsTest(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue

version in installJsdom := "16.4.0"

useYarn := true

requireJsDomEnv in Test := true

parallelExecution in Test := false

//scalaJSUseMainModuleInitializer := true

//scalaJSLinkerConfig in (Compile, fastOptJS) ~= { _.withSourceMap(false) }
