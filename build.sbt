import VersionHelper.{versionFmt, fallbackVersion}

// Lets me depend on Maven Central artifacts immediately without waiting
resolvers ++= Resolver.sonatypeOssRepos("public")

enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Versions.ScalaJsDom,
  "app.tulz" %%% "tuplez-full-light" % Versions.Tuplez,
  "com.raquo" %%% "ew" % Versions.Ew,
  "org.scalatest" %%% "scalatest" % Versions.ScalaTest % Test
)

// Replace default sbt-dynver version with a simpler one for easier local development
// ThisBuild / version ~= (_.replaceFirst("(\\+[a-z0-9-+]*-SNAPSHOT)", "-NEXT"))

// Makes sure to increment the version for local development
ThisBuild / version := dynverGitDescribeOutput.value
  .mkVersion(out => versionFmt(out, dynverSonatypeSnapshots.value), fallbackVersion(dynverCurrentDate.value))

ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer
    .getGitDescribeOutput(d)
    .mkVersion(out => versionFmt(out, dynverSonatypeSnapshots.value), fallbackVersion(d))
}

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

(Compile / scalacOptions) ~= (_.filterNot(Set(
  "-deprecation",
  "-Xfatal-warnings"
)))

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

(installJsdom / version) := Versions.JsDom

(webpack / version) := Versions.Webpack

(startWebpackDevServer / version) := Versions.WebpackDevServer

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
    GenerateTupleStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableStream(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableSignal(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateStaticStreamCombineOps(
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
    GenerateCombineStreamsTest(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue
