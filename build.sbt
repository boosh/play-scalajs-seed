name := """netmapper"""

version := "1.1-SNAPSHOT"

val scalaV = "2.11.8"

scalaVersion := scalaV

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val flyway = (project in file("modules/flyway"))
  .enablePlugins(FlywayPlugin)

lazy val models = (project in file("modules/models"))
  .enablePlugins(Common)

lazy val slick = (project in file("modules/slick"))
  .enablePlugins(Common)
  .aggregate(models)
  .dependsOn(models)

lazy val play = (project in file("modules/play")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(js),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.0.0"
//    specs2 % Test
  )
).enablePlugins(PlayScala)
  .aggregate(models, slick)
  .dependsOn(models, slick, sharedJvm)

lazy val js = (project in file("modules/js")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("modules/shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
