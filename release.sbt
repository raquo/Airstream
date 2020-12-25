name := "Airstream"

normalizedName := "airstream"

organization := "com.raquo"

scalaVersion := "2.13.4"

crossScalaVersions := Seq("2.12.12", "2.13.4")

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

