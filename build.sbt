ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

val AkkaVersion = "2.7.0"
val SlickVersion = "3.4.1"

libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.lightbend.akka" %% "akka-persistence-jdbc" % "5.2.0").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  "com.github.pjfanning" %% "jackson-module-scala3-enum" % "2.13.3",
  "dev.optics" %% "monocle-core"  % "3.1.0",
  "org.postgresql" % "postgresql" % "42.5.3",
  "ch.qos.logback" % "logback-classic" % "1.4.5"
)

lazy val root = (project in file("."))
  .settings(
    name := "game"
  )
