name := "pizza-killmail-analyser"

organization := "moe.pizza"

version := "0.2"

scalaVersion := "2.11.7"


resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "moe.pizza" %% "eveapi" % "0.20",
  "io.spray" %% "spray-caching" % "1.3.3"
)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)

bintrayReleaseOnPublish in ThisBuild := true

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
