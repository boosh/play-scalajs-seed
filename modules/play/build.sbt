
// Adding this means no explicit import in *.scala.html files
//TwirlKeys.templateImports += "com.example.model.UserDao"

resolvers += "Kaliber Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test"
)
