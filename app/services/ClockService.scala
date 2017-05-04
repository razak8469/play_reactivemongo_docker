package services
import javax.inject.Singleton

import com.google.inject.ImplementedBy
import org.joda.time.{DateTime, Days}

// clock service trait used by the user controller
@ImplementedBy(classOf[SystemClockService])
trait ClockService {
  def getDateTime(): DateTime

  def daysSinceLastLogged(lastLogged: DateTime): Int =
    Days.daysBetween(lastLogged, getDateTime()).getDays
}

// the system clock service, the current time given is the system current time
@Singleton
class SystemClockService extends ClockService {
  override def getDateTime() = DateTime.now
}

// fixed clock service, the current time given can be set to a desired time
@Singleton
class MockClockService(val date: DateTime) extends ClockService {
  var startTime = date
  override def getDateTime() = startTime

  def setDateTime(newDate: DateTime): Unit = startTime = newDate

}