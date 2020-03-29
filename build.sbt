enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "1.0.0", // This has no runtime cost. We only use it for `Debug.log` // @TODO[Elegance] Reconsider
  "org.scalatest" %%% "scalatest" % "3.1.1" % Test
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

version in installJsdom := "16.2.0"

useYarn := true

requireJsDomEnv in Test := true

parallelExecution in Test := false

scalaJSUseMainModuleInitializer := true

scalaJSLinkerConfig in (Compile, fastOptJS) ~= { _.withSourceMap(false) }

// @Warning remove this when scalajs-bundler > 0.17 is out https://github.com/scalacenter/scalajs-bundler/issues/332#issuecomment-594401804
Test / jsEnv := new tempfix.JSDOMNodeJSEnv(tempfix.JSDOMNodeJSEnv.Config((Test / installJsdom).value))
