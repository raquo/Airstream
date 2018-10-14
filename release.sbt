name := "Airstream"

normalizedName := "airstream"

organization := "com.raquo"

scalaVersion := "2.12.6"

// Scala 2.11 does not include Try.fold method which we use heavily.
// I don't think anyone would want to use Airstream / Laminar with 2.11 anyway.
// Let's see if anyone complains.
//crossScalaVersions := Seq("2.11.12", "2.12.6")

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
    url = url("http://raquo.com")
  )
)

sonatypeProfileName := "com.raquo"

publishMavenStyle := true

publishArtifact in Test := false

publishTo := sonatypePublishTo.value

releaseCrossBuild := true

pomIncludeRepository := { _ => false }

//useGpg := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

