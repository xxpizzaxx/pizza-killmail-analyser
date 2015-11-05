name := "pizza-killmail-analyser"

version := "1.0"

scalaVersion := "2.11.7"


resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "moe.pizza" %% "eveapi" % "0.19",
  "io.spray" %% "spray-caching" % "1.3.3"
)

