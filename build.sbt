ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

val AkkaVersion = "2.7.0"
val SlickVersion = "3.4.1"
val ScalaTestversion = "3.2.15"
val EnumeratumVersion = "1.7.2"

libraryDependencies ++= Seq(
  ("com.typesafe.akka"          %% "akka-persistence-typed"     % AkkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.lightbend.akka"         %% "akka-persistence-jdbc"      % "5.2.0").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka"          %% "akka-serialization-jackson" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  "com.github.pjfanning"        %% "jackson-module-scala3-enum" % "2.13.3",
  "dev.optics"                  %% "monocle-core"               % "3.1.0",
  "org.postgresql"              %  "postgresql"                 % "42.5.4",
  "ch.qos.logback"              %  "logback-classic"            % "1.4.5",
  "com.typesafe.scala-logging"  %% "scala-logging"              % "3.9.4",
  "org.jline"                   %  "jline"                      % "3.22.0",
//  "org.scalatest"               %% "scalatest"                  % ScalaTestversion  % "test",
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test cross CrossVersion.for3Use2_13,
)
//  .map(_.excludeAll(
//  ExclusionRule(organization = "org.scalacheck"),
//  ExclusionRule(organization = "org.scalactic"),
//  ExclusionRule(organization = "org.scalatest")
//))


libraryDependencies ++= Seq(
  "com.lihaoyi" %% "utest" % "0.8.1" % "test"
//  "org.scalatest" %% "scalatest" % ScalaTestversion % "test" cross CrossVersion.for3Use2_13,
//  "org.scalactic" %% "scalactic" % ScalaTestversion cross CrossVersion.for3Use2_13,
//    "org.scalatest" %% "scalatest" % ScalaTestversion % "test" cross CrossVersion.for3Use2_13,
//    "org.scalactic" %% "scalactic" % ScalaTestversion,
)

testFrameworks += new TestFramework("utest.runner.Framework")

lazy val root = (project in file("."))
  .settings(
    name := "game"
  )
