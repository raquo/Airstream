enablePlugins(ScalaJSPlugin)

ThisBuild / buildKitDownloads := Seq(
  _.fromGithubTag(
    repo = "raquo/scalafmt-config",
    filePath = ".scalafmt.shared.conf",
    tag = "v0.1.0"
  ).withDoNotEditComment(_.`#`)
)

mimaPreviousArtifacts := Set("com.raquo" %%% "airstream" % "17.2.0")


libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Versions.ScalaJsDom,
  "app.tulz" %%% "tuplez-full" % Versions.Tuplez,
  "com.raquo" %%% "ew" % Versions.Ew,
  "org.scalatest" %%% "scalatest" % Versions.ScalaTest % Test
)

// Auto-increment version for local development
ThisBuild / version := buildKitDynVer.version.value

ThisBuild / dynver := buildKitDynVer.dynver.value

scalaVersion := Versions.Scala_3

crossScalaVersions := Seq(Versions.Scala_2_13, Versions.Scala_3)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
)

scalacOptions ~= { options: Seq[String] =>
  options.filterNot(Set(
    "-Ywarn-value-discard",
    "-Wvalue-discard"
  ))
}

scalacOptions += pointScalaJsSourceMapsToGithub("raquo/Airstream").value

(Test / scalacOptions) ~= { options: Seq[String] =>
  options.filterNot { o =>
    o.startsWith("-Ywarn-unused") || o.startsWith("-Wunused")
  }
}

// (Compile / scalacOptions) ~= (_.filterNot(Set(
//   "-deprecation",
//   "-Xfatal-warnings"
// )))

(Compile / doc / scalacOptions) ~= (_.filterNot(
  Set(
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

jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

(Test / parallelExecution) := false

scalaJSUseMainModuleInitializer := true


// -- Code generators for N-arity functionality

val generateTupleCombinatorsFrom = 2
val generateTupleCombinatorsTo = 22

Compile / sourceGenerators += Def.task {
  Seq.concat(
    GenerateTupleStreams(
      classNamePattern = n => s"TupleStream$n",
      fileName = "TupleStreams.scala",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleSignals(
      classNamePattern = n => s"TupleSignal$n",
      fileName = "TupleSignals.scala",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineStreamOps(
      traitName = "CombineStreamOps",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineSignalOps(
      traitName = "CombineSignalOps",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineStreamObjectOps(
      traitName = "CombineStreamObjectOps",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineSignalObjectOps(
      traitName = "CombineSignalObjectOps",
      sourceDir = (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue

Test / sourceGenerators += Def.task {
  Seq.concat(
    GenerateCombineSignalsTest(
      className = "CombineSignalsSpec",
      testSourceDir = (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineStreamsTest(
      className = "CombineStreamsSpec",
      testSourceDir = (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue

// https://github.com/JetBrains/sbt-ide-settings
SettingKey[Seq[File]]("ide-excluded-directories").withRank(KeyRanks.Invisible) := Seq(
  ".buildkit", ".idea", ".metals", ".bloop", ".bsp",
  "target", "project/target", "project/project/target", "project/project/project/target",
  "node_modules"
).map(file)
