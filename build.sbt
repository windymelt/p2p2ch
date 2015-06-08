import play.Project._
import sbt.Keys._
import sbt.{Path, Resolver}

name := "P2P2ch"

version := "2.0"

scalaVersion := "2.11.6"

// コーディング規約チェッカプラグインscalastyleの設定をロード
org.scalastyle.sbt.ScalastylePlugin.Settings

resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += Resolver.file(
  "local-ivy-repos", file(Path.userHome + "/.ivy2/local")
)(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  "momijikawa" %% "p2pscalaproto" % "0.2.16",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-remote" % "2.2.3",
  "com.typesafe.akka" %% "akka-agent" % "2.2.3",
  "org.specs2" %% "specs2-core" % "3.1" % "test",
  "org.mockito" % "mockito-all" % "1.9.5",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "org.scalaz" %% "scalaz-effect" % "7.1.1",
  "org.scalaz" %% "scalaz-typelevel" % "7.1.1",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.1.1" % "test",
  jdbc,
  anorm
)

playScalaSettings

// テスト時にJUnitのXMLを出力させる設定
testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")
