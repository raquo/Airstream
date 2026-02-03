import com.raquo.buildkit.SourceDownloader
import VersionHelper.{versionFmt, fallbackVersion}

enablePlugins(ScalaJSPlugin)

lazy val preload = taskKey[Unit]("runs Airstream-specific pre-load tasks")

preload := {
  val projectDir = (ThisBuild / baseDirectory).value
  // TODO Move code generators here as well?

  SourceDownloader.downloadVersionedFile(
    name = "scalafmt-shared-conf",
    version = "v0.1.0",
    urlPattern = version => s"https://raw.githubusercontent.com/raquo/scalafmt-config/refs/tags/$version/.scalafmt.shared.conf",
    versionFile = projectDir / ".downloads" / ".scalafmt.shared.conf.version",
    outputFile = projectDir / ".downloads" / ".scalafmt.shared.conf",
    processOutput = "#\n# DO NOT EDIT. See SourceDownloader in build.sbt\n" + _
  )
}

Global / onLoad := {
  (Global / onLoad).value andThen { state => preload.key.label :: state }
}

mimaPreviousArtifacts := Set("com.raquo" %%% "airstream" % "17.2.0")


libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Versions.ScalaJsDom,
  "app.tulz" %%% "tuplez-full" % Versions.Tuplez,
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

scalacOptions ++= sys.env.get("CI").map { _ =>
  val localSourcesPath = (LocalRootProject / baseDirectory).value.toURI
  val remoteSourcesPath = s"https://raw.githubusercontent.com/raquo/Airstream/${git.gitHeadCommit.value.get}/"
  val sourcesOptionName = if (scalaVersion.value.startsWith("2.")) "-P:scalajs:mapSourceURI" else "-scalajs-mapSourceURI"

  s"${sourcesOptionName}:$localSourcesPath->$remoteSourcesPath"
}

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

// https://github.com/JetBrains/sbt-ide-settings
SettingKey[Seq[File]]("ide-excluded-directories").withRank(KeyRanks.Invisible) := Seq(
  ".downloads", ".idea", ".metals", ".bloop", ".bsp",
  "target", "project/target", "project/project/target", "project/project/project/target",
  "node_modules"
).map(file)
