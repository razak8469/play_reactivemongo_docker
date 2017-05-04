package models

import org.joda.time.DateTime
import play.api.libs.json._

case class User(name: String, high_score: Int, last_login: DateTime)
object User {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
  implicit val jodaDateReads = play.api.libs.json.Reads.jodaDateReads(dateFormat)
  implicit val jodaDateWrites = play.api.libs.json.Writes.jodaDateWrites(dateFormat)
  implicit val userFormat = Json.format[User]
}

case class UserWithId(id: Int, user: User)
object UserWithId {
  implicit val userWithIdFormat = Json.format[UserWithId]
}

