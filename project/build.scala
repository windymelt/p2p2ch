import play.Project._
import sbt.Keys._
import sbt._
import java.net.URI
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import org.scalastyle.sbt.ScalastylePlugin

object P2PScalaProto extends Build {

  lazy val P2PScalaProto =
    ProjectRef(new URI("https://github.com/windymelt/p2pScalaProto.git"), "P2PScalaProto")

  val Organization = "momijikawa"
  val Name = "P2P2ch"
  val Version = "1.10.0" 
  val ScalaVersion = "2.10.2" // Scalaバージョンは固定する

  // << groupId >> %%  << artifactId >> % << version >>
  lazy val LibraryDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.akka" %% "akka-remote" % "2.2.3",
    "com.typesafe.akka" %% "akka-agent" % "2.2.3",
    jdbc,
    anorm,
    cache % "test",
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
