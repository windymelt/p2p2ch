import play.Project._
import sbt.Keys._
import sbt._
import java.net.URI
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import org.scalastyle.sbt.ScalastylePlugin

object P2PScalaProto extends Build {

  lazy val P2PScalaProto =
    ProjectRef(new URI("https://github.com/Hiroyuki-Nagata/p2pScalaProto.git"), "P2PScalaProto")

  val Organization  = "momijikawa"
  val Name          = "P2P2ch"
  val Version       = "1.10.0" 
  val ScalaVersion  = "2.10.4"
  val ScalazVersion = "7.0.0"
  val AkkaVersion   = "2.2.3"

  // << groupId >> %%  << artifactId >> % << version >>
  lazy val LibraryDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor"  % AkkaVersion,
    "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
    "com.typesafe.akka" %% "akka-agent"  % AkkaVersion,
    "org.scalaz" %% "scalaz-core"               % ScalazVersion,
    "org.scalaz" %% "scalaz-effect"             % ScalazVersion,
    "org.scalaz" %% "scalaz-typelevel"          % ScalazVersion,
    "org.scalaz" %% "scalaz-scalacheck-binding" % ScalazVersion % "test",
    jdbc,
    anorm,
    cache % "test",
    "org.specs2" %% "specs2-core" % "3.1" % "test",
    "org.mockito" % "mockito-all" % "1.9.5",
    "commons-codec" % "commons-codec" % "1.9",
    "net.sourceforge.htmlunit" % "htmlunit" % "2.14" % "test",
    "org.apache.httpcomponents" % "httpclient" % "4.4",
    "org.apache.httpcomponents" % "httpcore" % "4.4" % "test"
  )
 
  lazy val projectSettings = Seq(
    parallelExecution in Test := false,
    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"),
    organization := Organization,
    name := Name,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += DefaultMavenRepository,
    resolvers += JavaNet1Repository,
    resolvers += Classpaths.typesafeReleases,
    resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/",
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= LibraryDependencies
  )

  lazy val project = Project(
    "P2P2ch",
    file("."),
    settings =
      Defaults.defaultSettings ++
        playScalaSettings ++
        projectSettings
  ) dependsOn (
    P2PScalaProto
  )
}
