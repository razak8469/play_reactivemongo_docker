package controllers

import javax.inject._

import models.{User, UserWithId}
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import services.{ClockService, UserDB}
import utils.Errors

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject() (val userDB: UserDB, val clockService: ClockService)(implicit val exec: ExecutionContext) extends Controller {

  // SINGLE CRUD OPERATIONS

  // retrieve a single user with the given id from the data store
  def getUserById(id: Int) = Action.async {
    val futureUserOpt: Future[Option[User]] = userDB.getUserById(id)
    futureUserOpt.map { userOpt =>
      userOpt match {
        case Some(user) => {
          val days_ago = clockService.daysSinceLastLogged(user.last_login)
          val jsRes = Json.toJson[User](user).as[JsObject] + ("days_ago" -> Json.toJson(days_ago))
          Ok(jsRes)
        }
        case None => Ok(s"No user exists with id: $id")
      }
    }
  }

  // insert or update a single user with the given id from the data store
  def insertOrUpdateUserById(id: Int) = Action.async(parse.tolerantJson) { request =>
    Json.fromJson[User](request.body) match {
      case JsSuccess(user, _) =>
        userDB.insertOrUpdateUserById(id, user).map { msg => Ok(msg) }
      case JsError(errors) =>
        Future.successful(BadRequest("Could not build a user from the json provided. " + Errors.show(errors)))
    }
  }

  // remove the user with the given id from the data store
  def removeUserById(id: Int) = Action.async {
    userDB.removeUserById(id).map { msg =>
      Ok(msg)
    }
  }

  // BULK CRUD OPERATIONS

  // retrieve all the users in the data store
  def getUsers() = Action.async {
    val futureUsersSeqOpt: Future[Option[List[User]]] = userDB.getUsers()
    futureUsersSeqOpt.map { userSeqOpt =>
      userSeqOpt match {
        case Some(userSeq) =>
          val jsObjList = userSeq map {user =>
            val days_ago = clockService.daysSinceLastLogged(user.last_login)
            Json.toJson[User](user).as[JsObject] + ("days_ago" -> Json.toJson(days_ago))
          }
          Ok(jsObjList.mkString(","))
        case None => Ok("No users exist")
      }
    }
  }

  // insert or update many users into the data store. The id of the user must be provided in the json body
  def insertOrUpdateUsers() = Action.async(parse.tolerantJson) { request =>
    Json.fromJson[Seq[UserWithId]](request.body) match {
      case JsSuccess(users, _) =>
        userDB.insertOrUpdateUsers(users) map { msg => Ok(msg)}
      case JsError(errors) =>
        Future.successful(BadRequest("Could not build user from the json provided. " + Errors.show(errors)))
    }
  }

  // remove a selection of users from the data store.
  def removeUsers() = Action.async(parse.tolerantJson){ request =>
    val jsonBody = request.body
    val fromOpt = (jsonBody \ "fromId").asOpt[Int]
    val toOpt = (jsonBody \ "toId").asOpt[Int]
    userDB.removeUsers(fromOpt, toOpt) map {msg => Ok(msg)}
  }

}