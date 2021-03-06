import sbt.{AutoPlugin, Resolver}
import sbt.plugins.JvmPlugin
import sbt.Keys._
import sbt.{Resolver, _}

object Common extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def projectSettings = Seq(
    scalaVersion := "2.11.7",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", // yes, this is 2 args
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-numeric-widen",
      "-Xfatal-warnings"
    ),
    resolvers ++= Seq(
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
       Resolver.jcenterRepo,
       Resolver.sonatypeRepo("releases"),
       Resolver.sonatypeRepo("snapshots")),
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "javax.inject" % "javax.inject" % "1",
      "joda-time" % "joda-time" % "2.9.2",
      "org.joda" % "joda-convert" % "1.2",
      "com.google.inject" % "guice" % "4.0",
      "com.mohiva" %% "play-silhouette" % "4.0.0",
      "com.mohiva" %% "play-silhouette-persistence" % "4.0.0"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )
}
