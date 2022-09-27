enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Versions.ScalaJsDom,
  "app.tulz" %%% "tuplez-full-light" % Versions.Tuplez,
  "org.scalatest" %%% "scalatest" % Versions.ScalaTest % Test
)

scalaVersion := Versions.Scala_2_13

crossScalaVersions := Seq(Versions.Scala_2_12, Versions.Scala_2_13, Versions.Scala_3)

scalacOptions ~= { options: Seq[String] =>
  options.filterNot(Set(
    "-Ywarn-value-discard",
    "-Wvalue-discard"
  ))
}

scalacOptions += {
  val localSourcesPath = baseDirectory.value.toURI
  val remoteSourcesPath = s"https://raw.githubusercontent.com/raquo/Airstream/${git.gitHeadCommit.value.get}/"
  val sourcesOptionName = if (scalaVersion.value.startsWith("2.")) "-P:scalajs:mapSourceURI" else "-scalajs-mapSourceURI"

  s"${sourcesOptionName}:$localSourcesPath->$remoteSourcesPath"
}

(Test / scalacOptions) ~= { options: Seq[String] =>
  options.filterNot { o =>
    o.startsWith("-Ywarn-unused") || o.startsWith("-Wunused")
  }
}

// #TODO #nc re-enable deprecation warnings for 15.0.0 !!!!!!! Disabling for now to get around scala-js-dom's deprecation of dom.ext.Ajax
(Compile / scalacOptions) ~= (_.filterNot(Set(
  //"-deprecation",
  "-Xfatal-warnings"
)))

(Compile / doc / scalacOptions) ~= (_.filterNot(
  Set(
    "-scalajs",
    "-deprecation",
    "-explain-types",
    "-explain",
    "-feature",
    "-language:existentials,experimental.macros,higherKinds,implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ykind-projector",
    "-from-tasty",
    "-encoding",
    "utf8",
  )
))

(Compile / doc / scalacOptions) ++= Seq(
  "-no-link-warnings" // Suppress scaladoc "Could not find any member to link for" warnings
)

(installJsdom / version) := Versions.JsDom

useYarn := true

(Test / requireJsDomEnv) := true

(Test / parallelExecution) := false

scalaJSUseMainModuleInitializer := true

(Compile / fastOptJS / scalaJSLinkerConfig) ~= { _.withSourceMap(false) }


// -- Code generators for N-arity functionality

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
