enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.8", // This has no runtime cost. We only use it for `Debug.log` // @TODO[Elegance] Reconsider
  "org.scalatest" %%% "scalatest" % "3.1.0" % Test
)

scalacOptions ++= Seq(
  // "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions"
)

// @TODO[Build] Why does this need " in (Compile, doc)" while other options don't?
scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppress scaladoc "Could not find any member to link for" warnings
)

version in installJsdom := "16.0.1"

useYarn := true

requireJsDomEnv in Test := true

// parallelExecution in Test := false

scalaJSUseMainModuleInitializer := true

emitSourceMaps := false

