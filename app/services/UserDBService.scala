package services

import javax.inject._

import com.google.inject.ImplementedBy
import models.{User, UserWithId}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands._
import reactivemongo.play.json.JSONSerializationPack
import reactivemongo.play.json.collection.JSONCollection

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}

// trait used by the user controller for data store CRUD operations, by default the mongodb module is used
@ImplementedBy(classOf[MongoUserDB])
trait UserDB {
  def getUserById(id: Int): Future[Option[User]]
  def insertOrUpdateUserById(id: Int, user: User): Future[String]
  def insertOrUpdateUsers(usersWithId: Seq[UserWithId]): Future[String]
  def getUsers(): Future[Option[List[User]]]
  def removeUserById(id: Int): Future[String]
  def removeUsers(fromIdOpt: Option[Int] = None, toIdOpt: Option[Int] = None): Future[String]
}

// monogdb module that interacts with the mongodb database
@Singleton
class MongoUserDB @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit val exec: ExecutionContext)
  extends UserDB with MongoController with ReactiveMongoComponents {

  // the collection of the mongodb interacted with
  def usersFuture: Future[JSONCollection] = database.map(_.collection[JSONCollection]("users"))

  // retrieve a single user by id
  override def getUserById(id: Int): Future[Option[User]] = {
    val futureUsersList: Future[List[UserWithId]] = usersFuture.flatMap {
      _.find(Json.obj("id" -> id)).
        cursor[UserWithId](ReadPreference.primary).
        collect[List]()
    }
    futureUsersList.map { usersWithId =>
      if(usersWithId.isEmpty) None else Some(usersWithId.head.user)
    }
  }

  // insert or update a single user by id
  override def insertOrUpdateUserById(id: Int, user: User): Future[String] = for {
      users <- usersFuture
      lastError <- users.update(Json.obj("id" -> id), UserWithId(id, user), upsert = true)
    } yield lastError.errmsg.getOrElse(s"Upserted user with id: $id")

  // remove a single user by id
  override def removeUserById(id: Int): Future[String] = for {
    users <- usersFuture
    lastError <- users.remove(Json.obj("id" -> id))
  } yield lastError.errmsg.getOrElse(s"Removed user with id: $id")

  // retrieve all the users in the collection
  override  def getUsers(): Future[Option[List[User]]] = {

    val futureUsersList: Future[List[UserWithId]] = usersFuture.flatMap {
      _.find(Json.obj()).cursor[UserWithId](ReadPreference.primary).collect[List]()
    }
    futureUsersList.map { usersWithId =>
      if(usersWithId.isEmpty) None else Some(usersWithId.map{_.user})
    }
  }

  // insert or update many users into the collection
  override def insertOrUpdateUsers(users: Seq[UserWithId]): Future[String] = {
    // use the latest by last login when users with the same id present
      val latestLoggedUsers = users.groupBy(_.id).transform { (id, usersWithSameId) =>
            usersWithSameId.reduce { (u1, u2) =>
              u1.user.last_login.isAfter(u2.user.last_login) match {
                case true => u1
                case false => u2
              }
            }}.map{case(_, userWithId) => userWithId}

      def singleUpdate(userWithId: UserWithId) = Json.obj(
        ("q" -> Json.obj("id" -> userWithId.id)),
        ("u" -> Json.obj("$set" -> Json.toJson(userWithId))),
        ("upsert" -> true)
      )
      val commandDoc = Json.obj(
        "update" -> "users",
        "updates" -> {for(user <- latestLoggedUsers) yield singleUpdate(user)},
        "ordered" -> false
      )
      val runner = Command.run(JSONSerializationPack)
      val rawCommand = runner.rawCommand(commandDoc)

      for {
        users <- usersFuture
        res <- runner.apply(users.db, runner.rawCommand(commandDoc)).one[JsObject]
      } yield {
        res.toString()
      }
  }

  // remove a selection or all  of the users from the collection
  override def removeUsers(fromIdOpt: Option[Int] = None, toIdOpt: Option[Int] = None): Future[String] = for {
    users <- usersFuture
    lastError <- (fromIdOpt, toIdOpt) match {
      case (None, None) =>
        users.remove(Json.obj())
      case (Some(fromId), None) =>
        users.remove(Json.obj("id" -> Json.obj("$gte" -> fromId)))
      case (None, Some(toId)) =>
        users.remove(Json.obj("id" -> Json.obj("$lte" -> toId)))
      case (Some(fromId), Some(toId)) =>
        users.remove(Json.obj("id" -> Json.obj(("$gte" -> fromId), ("$lte" -> toId))))
    }
  } yield lastError.errmsg.getOrElse(s"Successfully removed ${lastError.n} users")

}

// an in memory data store
@Singleton
class InMemoryUserDB @Inject()(implicit val exec: ExecutionContext) extends UserDB {
  // internal data store
  private val db: mutable.Map[Int, User] = mutable.Map()

  // retrieve a single user
  override def getUserById(id: Int): Future[Option[User]] =
    Future.successful{ db.get(id)}

  // insert or update a user by id
  override def insertOrUpdateUserById(id: Int, user: User): Future[String] = {
    db.get(id) match {
      case None =>
        db += (id -> user)
        Future.successful{s"Added user with id: $id "}
      case _ =>
        db(id) = user
        Future.successful{s"Updated user with id: $id "}
    }
  }

  // delete a user with the specified id
  override def removeUserById(id: Int): Future[String] = {
    db.get(id) match {
      case None =>
        Future.successful(s"No user exists with id: $id")
      case _ =>
        db -= id
        Future.successful{s"Removed user with id: $id "}
    }
  }

  // retrieve all the users in the collection
  override  def getUsers(): Future[Option[List[User]]] = Future.successful {
      if(db.isEmpty) None
      else {Some(db.values.toList)}
    }

  // insert or update many users
  override def insertOrUpdateUsers(users: Seq[UserWithId]): Future[String] = {
    // use the latest by last login when users with the same id present
    val latestLoggedUsers = users.groupBy(_.id).transform { (id, usersWithSameId) =>
      usersWithSameId.reduce { (u1, u2) =>
        u1.user.last_login.isAfter(u2.user.last_login) match {
          case true => u1
          case false => u2
        }
      }}.map{case(_, userWithId) => userWithId}

    var inserted = 0
    var updated = 0

    latestLoggedUsers.map{ case userWithId =>
      val id = userWithId.id
      db.get(id) match {
        case None =>
          db += (id -> userWithId.user)
          inserted += 1
        case _ =>
          db(id) = userWithId.user
          updated += 1
      }
    }
    Future.successful(s"inserted ${inserted} users and updated ${updated} users")
  }

  // remove a selection or all of the users
  override def removeUsers(fromIdOpt: Option[Int] = None, toIdOpt: Option[Int] = None): Future[String] = {
    var deleted = 0
    (fromIdOpt, toIdOpt) match {
      case (None, None) =>
        deleted = db.size
        db.clear()

      case (Some(fromId), Some(toId)) =>
        val deletedKeys = db.keys.filter(key => key >= fromId && key <= toId ).toList
        deleted = deletedKeys.size
        db --= deletedKeys

      case (Some(fromId), None) =>
        val deletedKeys = db.keys.filter(key => key >= fromId).toList
        deleted = deletedKeys.size
        db --= deletedKeys

      case (None, Some(toId)) =>
        val deletedKeys = db.keys.filter(key => key <= toId).toList
        deleted = deletedKeys.size
        db --= deletedKeys
    }
    Future.successful(s"removed $deleted users")
  }

}