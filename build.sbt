enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.7", // This has no runtime cost. We only use it for `Debug.log` // @TODO[Elegance] Reconsider
  "org.scalatest" %%% "scalatest" % "3.0.8" % Test
)

useYarn := true

requiresDOM in Test := true

// parallelExecution in Test := false

scalaJSUseMainModuleInitializer := true

emitSourceMaps := false

