name := "Airstream"

normalizedName := "airstream"

organization := "com.raquo"

scalaVersion := "0.27.0-RC1"

crossScalaVersions := Seq("2.12.11", "2.13.1", "0.27.0-RC1")

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

