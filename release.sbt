name := "Airstream"

normalizedName := "airstream"

organization := "com.raquo"

homepage := Some(url("https://github.com/raquo/Airstream"))

licenses += ("MIT", url("https://github.com/raquo/Airstream/blob/master/LICENSE.md"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/raquo/Airstream"),
    "scm:git@github.com/raquo/Airstream.git"
  )
)

developers := List(
  Developer(
    id = "raquo",
    name = "Nikita Gazarov",
    email = "nikita@raquo.com",
    url = url("https://github.com/raquo")
  )
)

sonatypeProfileName := "com.raquo"

publishMavenStyle := true

(Test / publishArtifact) := false

publishTo := sonatypePublishToBundle.value

releaseCrossBuild := true

pomIncludeRepository := { _ => false }

releaseProcess := {
  import ReleaseTransformations._
  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}

//useGpg := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

