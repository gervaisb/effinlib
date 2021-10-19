resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

name := "effinlib"
version := "1.0"

scalaVersion := "2.12.2"

lazy val AkkaStream = "com.typesafe.akka" %% "akka-stream" % "2.6.1"
lazy val Jumblr = "com.tumblr" % "jumblr" % "0.0.13"
lazy val Redis = "net.debasishg" %% "redisclient" % "3.20"
lazy val Guice = guice
lazy val PlayWsClient = ws

lazy val `effinlib` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      AkkaStream,
      Jumblr,
      Redis,
      Guice,
      PlayWsClient
    )
  )

unmanagedResourceDirectories in Test <+= baseDirectory (_ / "target/web/public/test")






