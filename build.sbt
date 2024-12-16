ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .settings(
    name := "taro_backend",
    idePackagePrefix := Some("ru.otus")
  )

libraryDependencies ++= Dependencies.zio
libraryDependencies ++= Dependencies.zioTest
libraryDependencies ++= Dependencies.postgres
libraryDependencies ++= Dependencies.liquibase
libraryDependencies ++= Dependencies.scalaTest
libraryDependencies ++= Dependencies.enumeratum
libraryDependencies ++= Dependencies.log4j
libraryDependencies ++= Dependencies.bcrypt
libraryDependencies ++= Dependencies.zioScala

scalacOptions += "-Ymacro-annotations"