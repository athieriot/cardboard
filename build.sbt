ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

val AkkaVersion = "2.7.0"
val AkkaHttpVersion = "10.5.0"
val AkkPersistenceVersion = "5.2.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-persistence-typed"     % AkkaVersion           cross CrossVersion.for3Use2_13,
  "com.lightbend.akka"          %% "akka-persistence-jdbc"      % AkkPersistenceVersion cross CrossVersion.for3Use2_13,
  "com.typesafe.akka"           %% "akka-serialization-jackson" % AkkaVersion           cross CrossVersion.for3Use2_13,
  "com.typesafe.akka"           %% "akka-http"                  % AkkaHttpVersion       cross CrossVersion.for3Use2_13,
  "com.typesafe.akka"           %% "akka-http-spray-json"       % AkkaHttpVersion       cross CrossVersion.for3Use2_13,
  "ch.megard"                   %% "akka-http-cors"             % "1.2.0",
  "com.github.pjfanning"        %% "jackson-module-scala3-enum" % "2.13.3",
  "dev.optics"                  %% "monocle-core"               % "3.1.0",
  "org.postgresql"              %  "postgresql"                 % "42.5.4",
  "ch.qos.logback"              %  "logback-classic"            % "1.4.5",
  "com.typesafe.scala-logging"  %% "scala-logging"              % "3.9.5",
  "org.jline"                   %  "jline"                      % "3.22.0",
)


libraryDependencies ++= Seq(
  "com.lihaoyi"                 %% "utest"                      % "0.8.1"     % Test,
  "org.scalacheck"              %% "scalacheck"                 % "1.17.0"    % Test cross CrossVersion.for3Use2_13,
  "com.typesafe.akka"           %% "akka-persistence-testkit"   % AkkaVersion % Test cross CrossVersion.for3Use2_13,
)

testFrameworks += new TestFramework("utest.runner.Framework")

lazy val root = (project in file("."))
  .settings(
    name := "game"
  )
