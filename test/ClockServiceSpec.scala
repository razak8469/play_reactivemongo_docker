import models.User
import org.joda.time.DateTime
import org.scalatestplus.play._
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._

class ClockServiceSpec extends PlaySpec with OneAppPerTest {

  val logged3daysAgo = DateTime.now().minusDays(3)
  // a dummy user for testing with last_login exactly as 3 days before the current date and time
  val dummyUser = User("dummyUser", 69, logged3daysAgo)

  "The UserController injected with the mock clock service" should {
    val mockClockService = new MockClockService(DateTime.now)

    // binding the mocked clock for guice injection to the application
    val application = new GuiceApplicationBuilder()
      .overrides(bind[UserDB].to[InMemoryUserDB])
      .overrides(bind[ClockService].toInstance(mockClockService))
      .build

    //testing the insertion of the dummy user
    "add a user set with last_login as 3 days ago" in {
      val fakeRequest = FakeRequest("PUT", "/users/user_id=1").withJsonBody(Json.toJson[User](dummyUser))
      val Some(result) = route(application, fakeRequest)
      status(result) mustEqual OK
      contentAsString(result) mustBe "Added user with id: 1 "
    }

    // testing the time delta with a mocked current time and last_login
    "give a time delta of 2 instead of 3 with current day set to yesterday" in {
      val yesterday = DateTime.now().minusDays(1)
      mockClockService.setDateTime(yesterday)
      val Some(result) = route(application, FakeRequest(GET, "/users/user_id=1"))
      status(result) mustEqual OK
      (contentAsJson(result) \ "days_ago").as[Int] mustBe 2
    }

    // testing the time delta for 24 hr clock timings
    // with current time set to yesterday minus 1 min, the time delta is 47 hrs and 59 min
    // and with fractional days ignored should return 1 day
    "give a time delta of 1 instead of 2 with current day set to yesterday and time 1 minute less than current time" in {
      val yesterdayMinusAMinute = DateTime.now().minusDays(1).minusMinutes(1)
      mockClockService.setDateTime(yesterdayMinusAMinute)
      val Some(result) = route(application, FakeRequest(GET, "/users/user_id=1"))
      status(result) mustEqual OK
      (contentAsJson(result) \ "days_ago").as[Int] mustBe 1
    }

    // testing the time delta with a different mocked current time and last_login
    "give a time delta of 1 instead of 2 with current day set to 2 days ago" in {
      val twoDaysAgo = DateTime.now().minusDays(2)
      mockClockService.setDateTime(twoDaysAgo)
      val Some(result) = route(application, FakeRequest(GET, "/users/user_id=1"))
      status(result) mustEqual OK
      (contentAsJson(result) \ "days_ago").as[Int] mustBe 1
    }
  }

  "The User controller injected with the system clock" should {
    // binding system clock for guice injection to the application
    val application = new GuiceApplicationBuilder()
      .overrides(bind[UserDB].to[InMemoryUserDB])
      .overrides(bind[ClockService].to[SystemClockService])
      .build

    //testing the insertion of the dummy user
    "add a user set with last_login as 3 days ago" in {
      val fakeRequest = FakeRequest("PUT", "/users/user_id=1").withJsonBody(Json.toJson[User](dummyUser))
      val Some(result) = route(application, fakeRequest)
      status(result) mustEqual OK
      contentAsString(result) must include("Added user with id: 1")
    }

    // testing the time delta with a mocked current time and last_login
    "give the time delta as 3 for the days since the user lasted logged in" in {
      val Some(result) = route(application, FakeRequest(GET, "/users/user_id=1"))
      status(result) mustEqual OK
      (contentAsJson(result) \ "days_ago").as[Int] mustBe 3
    }
  }
}
