name := "akka-monitoring"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
libraryDependencies ++= Seq(
  "io.kamon" %% "kamon-core" % "1.1.0",
  "io.kamon" %% "kamon-logback" % "1.0.0",
  "io.kamon" %% "kamon-akka-2.5" % "1.0.1",
  "io.kamon" %% "kamon-prometheus" % "1.0.0",
  "io.kamon" %% "kamon-zipkin" % "1.0.0"
)
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.12"