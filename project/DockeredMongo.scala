import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object DockeredMongo {
  def apply(base: File): PlayRunHook = {
    object MongoContainer extends PlayRunHook {
      // build the docker imgae befre the application run
      override def beforeStarted(): Unit = {
        Process("docker build --tag play_reactive_mongo/mongo . ", base).run
      }
    
      // bring up a mongodb instance container in the image
      override def afterStarted(addr: InetSocketAddress): Unit = {
        Process("docker run -p 27017:27017 --name userdb_mongo_instance -d play_reactive_mongo/mongo --smallfiles", base).run
      }

      // remove the mongodb instance container on exit of the application
      override def afterStopped(): Unit = {
        Process("docker rm -f userdb_mongo_instance", base).run
        Thread.sleep(2000)
      }
    }
    MongoContainer
  }
}
