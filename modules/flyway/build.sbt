

// Database Migrations:
// run with "sbt flywayMigrate"
// http://flywaydb.org/getstarted/firststeps/sbt.html

//$ export DB_DEFAULT_URL="jdbc:h2:/tmp/example.db"
//$ export DB_DEFAULT_USER="sa"
//$ export DB_DEFAULT_PASSWORD=""

libraryDependencies += "org.flywaydb" % "flyway-core" % "4.0"

lazy val databaseUrl = sys.env.getOrElse("DB_DEFAULT_URL", "jdbc:postgresql://localhost:5432/play")
lazy val databaseUser = sys.env.getOrElse("DB_DEFAULT_USER", "play")
lazy val databasePassword = sys.env.getOrElse("DB_DEFAULT_PASSWORD", "testpass")

flywayLocations := Seq("classpath:db/migration")

flywayUrl := databaseUrl
flywayUser := databaseUser
flywayPassword := databasePassword
flywayBaselineOnMigrate := true