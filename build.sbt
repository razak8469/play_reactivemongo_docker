name := """play_reactivemongo_docker"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.reactivemongo" % "play2-reactivemongo_2.11" % "0.11.14",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.11.14",
  "joda-time" % "joda-time" % "2.9.9",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

// directory for the docker file that installs mongo db
lazy val dockerDirectory = baseDirectory {
  _ / "infrastructure"
}

// play run-time hook hook to have the mongo-db running in a docker container
PlayKeys.playRunHooks <+= dockerDirectory.map(path => DockeredMongo(path))

