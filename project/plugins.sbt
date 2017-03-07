resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Flyway" at "https://flywaydb.org/repo"

// Database migration
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0")

// Slick code generation
// https://github.com/tototoshi/sbt-slick-codegen
addSbtPlugin("com.github.tototoshi" % "sbt-slick-codegen" % "1.2.0")

libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

// Scala JS
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.14")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")

// assembly to build fat jars
//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
