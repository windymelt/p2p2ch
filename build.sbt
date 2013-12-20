import play.Project._
import sbt.Keys._
import sbt.{Path, Resolver}

name := "P2P2ch"

version := "1.05"

resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/"

resolvers += Resolver.file(
  "local-ivy-repos", file(Path.userHome + "/.ivy2/local")
)(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  "momijikawa" %% "p2pscalaproto" % "0.2.9",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-remote" % "2.2.3",
  "com.typesafe.akka" %% "akka-agent" % "2.2.3",
  jdbc,
  anorm
)

playScalaSettings
