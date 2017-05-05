import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object DockeredMongo {
  def apply(base: File): PlayRunHook = {
    object MongoContainer extends PlayRunHook {
      // build the docker ubuntu imgae and bring up a mongodb instance container in the imagee
      override def beforeStarted(): Unit = {
         val dockerProcess = Process("docker build --tag play_reactive_mongo/mongo . ", base) #&&
            Process("docker rm -f userdb_mongo_instance", base) ###
            Process("docker run -p 27017:27017 --name userdb_mongo_instance -d play_reactive_mongo/mongo --smallfiles", base)
          dockerProcess.run 
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
