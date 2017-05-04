import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object DockeredMongo {
  def apply(base: File): PlayRunHook = {
    object MongoContainer extends PlayRunHook {
      override def beforeStarted(): Unit = {
        Process("docker build --tag play_reactive_mongo/mongo . ", base).run
      }

      override def afterStarted(addr: InetSocketAddress): Unit = {
        Process("docker run -p 27017:27017 --name userdb_mongo_instance -d play_reactive_mongo/mongo --smallfiles", base).run
      }

      override def afterStopped(): Unit = {
        Process("docker rm -f userdb_mongo_instance", base).run
        Thread.sleep(2000)
      }
    }
    MongoContainer
  }
}