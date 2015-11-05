name := "pizza-killmail-analyser"

version := "1.0"

scalaVersion := "2.11.7"


resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "moe.pizza" %% "eveapi" % "0.17"
)

