ThisBuild / organization := "com.raquo"
ThisBuild / homepage := Some(url("https://github.com/raquo/Airstream"))
ThisBuild / licenses += "MIT" -> url("https://github.com/raquo/Airstream/blob/master/LICENSE.md")
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/raquo/Airstream"), "scm:git@github.com/raquo/Airstream.git"))
ThisBuild / developers += Developer("raquo", "Nikita Gazarov", "nikita@raquo.com", url("http://raquo.com"))
ThisBuild / sonatypeProfileName := "com.raquo"
ThisBuild / publishArtifact in Test := false
ThisBuild / publishTo := sonatypePublishToBundle.value

