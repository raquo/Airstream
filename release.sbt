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

(Test / publishArtifact) := false

pomIncludeRepository := { _ => false }

sonatypeCredentialHost := "s01.oss.sonatype.org"

sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

