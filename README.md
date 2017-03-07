# Play 2.5 with Scala JS cross building

This repo is a seed project for using ScalaJS cross-building with Play 2.5. Database schemas are auto-generated with Slick, and Flyway is used for migrations.

The seed repos for this project were: 

  - https://github.com/playframework/play-isolated-slick/
  - https://github.com/vmunier/play-with-scalajs-example

Refer to those for more information and examples. 

Note: Some features have been commented out. Refer to the above to see how to enable the database integration for example.

# Development notes

Read below for more info on how this project is laid out. However, here are a few notes for development:

* The slickCodegen sbt task sometimes says it's written output to Tables.scala but it hasn't. In that case, running `sbt "project play" compile` seems to fix the issue.
* Once `Tables.scala` has been generated, edit the project structure in intellij and stop the `slick/target` directory from being excluded. Intellij needs to treat `Tables.scala` as a source file to make code that uses it load correctly.  

This project is configured to keep all the modules self-contained.  

* Slick is isolated from Play, not using play-slick.  
* Database migration is done using [Flyway](https://flywaydb.org/), not Play Evolutions.
* Slick's classes are auto-generated following database migration.

## Database

The application is configured to use PostgreSQL and has some custom drivers to make Postgres / Slick integration easier.

If you are using PostgreSQL for the first time, follow the instructions to [install on Mac using HomeBrew](http://exponential.io/blog/2015/02/21/install-postgresql-on-mac-os-x-via-brew/), and then start up PostgreSQL.

```
pg_ctl -D /usr/local/var/postgres start
```

```
sudo su - postgres # if on Linux
```
Then:
```
createdb myapp
createuser --pwprompt myuser
```

### Database Migration

The first thing to do is to run the database scripts.  Flyways has a number of advantages over Play Evolutions: it allows for both Java migrations and SQL migrations, and has command line support.  

Start up `sbt` and go into the flyway module to run database migrations:

```
project flyway
flywayMigrate
```

See [the flyways documentation](http://flywaydb.org/getstarted/firststeps/sbt.html) for more details.

## Slick 

The Postgres Driver for Slick is configured with [slick-pg](https://github.com/tminglei/slick-pg), which allows for custom mapping between PostgreSQL data types and Joda Time data types:

```scala
trait MyPostgresDriver extends ExPostgresDriver
with PgArraySupport
with PgDateSupportJoda
with PgPlayJsonSupport {

  object MyAPI extends API with DateTimeImplicits with JsonImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }

  // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"
  def pgjson = "jsonb"

  override val api = MyAPI
}

object MyPostgresDriver extends MyPostgresDriver
```

Slick configuration is simple, because the Slick [schema code generation](http://slick.typesafe.com/doc/3.1.0/code-generation.html) will look at the tables created from Flyway, and automatically generate a `Tables` trait.  From there, `UsersRow` and `Users` are created automatically.  Some conversion code is necessary to map between `UsersRow` and `User`. 

```scala
@Singleton
class SlickUserRepo @Inject()(db: Database) extends UserRepo with Tables {

  // Use the custom postgresql driver.
  override val profile: JdbcProfile = MyPostgresDriver

  import profile.api._

  private val queryById = Compiled(
    (id: Rep[Int]) => Users.filter(_.id === id))

  def lookup(id: Int)(implicit ec: UserRepoExecutionContext): Future[Option[User]] = {
    val f: Future[Option[UsersRow]] = db.run(queryById(id).result.headOption)
    f.map(maybeRow => maybeRow.map(usersRowToUser(_)))
  }

  def all(implicit ec: UserRepoExecutionContext): Future[Seq[User]] = {
    val f = db.run(Users.result)
    f.map(seq => seq.map(usersRowToUser(_)))
  }

  def update(user: User)(implicit ec: UserRepoExecutionContext): Future[Int] = {
    db.run(queryById(user.id).update(userToUsersRow(user)))
  }

  def delete(id: Int)(implicit ec: UserRepoExecutionContext): Future[Int] = {
    db.run(queryById(id).delete)
  }

  def create(user: User)(implicit ec: UserRepoExecutionContext): Future[Int] = {
    db.run(
      Users += userToUsersRow(user.copy(createdAt = DateTime.now()))
    )
  }

  def close(): Future[Unit] = {
    Future.successful(db.close())
  }

  private def userToUsersRow(user:User): UsersRow = {
    UsersRow(user.id, user.email, user.createdAt, user.updatedAt)
  }

  private def usersRowToUser(usersRow:UsersRow): User = {
    User(usersRow.id, usersRow.email, usersRow.createdAt, usersRow.updatedAt)
  }
}
```

Once `SlickUserRepo` is compiled, everything is available to be bound and run in the Play application.

## Play

The root `Module.scala` file contains all the classes need to bind Slick and expose it as a `UserRepo`:

```scala
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {

    bind(classOf[Config]).toInstance(configuration.underlying)
    bind(classOf[UserRepoExecutionContext]).toProvider(classOf[SlickUserRepoExecutionContextProvider])

    bind(classOf[slick.jdbc.JdbcBackend.Database]).toProvider(classOf[DatabaseProvider])
    bind(classOf[UserRepo]).to(classOf[SlickUserRepo])

    bind(classOf[UserRepoCloseHook]).asEagerSingleton()
  }
}
```

There are a couple of providers to do a "lazy get" of the database and execution context from configuration:

```scala
@Singleton
class DatabaseProvider @Inject() (config: Config) extends Provider[slick.jdbc.JdbcBackend.Database] {

  private val db = slick.jdbc.JdbcBackend.Database.forConfig("myapp.database", config)

  override def get(): slick.jdbc.JdbcBackend.Database = db
}

@Singleton
class SlickUserRepoExecutionContextProvider @Inject() (actorSystem: akka.actor.ActorSystem) extends Provider[UserRepoExecutionContext] {
  private val instance = {
    val ec = actorSystem.dispatchers.lookup("myapp.database-dispatcher")
    new SlickUserRepoExecutionContext(ec)
  }

  override def get() = instance
}

class SlickUserRepoExecutionContext(ec: ExecutionContext) extends UserRepoExecutionContext {
  override def execute(runnable: Runnable): Unit = ec.execute(runnable)

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
```

The Repo must be closed to release JDBC connections, and this is handled through `UserRepoCloseHook`:

```scala
class UserRepoCloseHook @Inject()(repo: UserRepo, lifecycle: ApplicationLifecycle) {
  private val logger = org.slf4j.LoggerFactory.getLogger("application")

  lifecycle.addStopHook { () =>
    Future.successful {
      logger.info("Now closing database connections!")
      repo.close()
    }
  }
}
```

From there, the controller code is simple:

```scala
@Singleton
class HomeController @Inject() (userRepo: UserRepo, userRepoExecutionContext: UserRepoExecutionContext) extends Controller {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  implicit val ec = userRepoExecutionContext

  def index = Action.async {
    logger.info("Calling index")
    userRepo.all.map { users =>
      logger.info(s"Calling index: users = ${users}")
      Ok(views.html.index(users))
    }
  }
}
```

## Running

To run the project, start up Play:

```
project play
run
```

And that's it! 
 
Now go to [http://localhost:9000](http://localhost:9000), and you will see the list of users in the database.
