import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import java.net.URI
import org.scalastyle.sbt.ScalastylePlugin
import play.Project._
import sbt.Keys._
import sbt._
import scalariform.formatter.preferences._

object P2P2ch extends Build {

  lazy val P2PScalaProto =
    ProjectRef(new URI("https://github.com/windymelt/p2pScalaProto.git"), "P2PScalaProto")

  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignParameters, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(SpacesWithinPatternBinders, true)

  val Organization  = "momijikawa"
  val Name          = "P2P2ch"
  val Version       = "1.10.0" 
  val ScalaVersion  = "2.10.4"
  val ScalazVersion = "7.1.1"
  val AkkaVersion   = "2.2.3"
  val Specs2Version = "3.1"

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
    // Scalaのバイナリ互換性の無さには吐き気がする
    // http://d.hatena.ne.jp/xuwei/20140825/1408979619
    // play-testがSpecs2 2.1.1を引っ張ってくるのでここで止める
    "com.typesafe.play" %% "play-test" % "2.2.1" % "test" excludeAll(
      ExclusionRule(organization = "org.specs2")
    ),
    // Specs2の2.4.0以降はscalaz7.1.xに依存
    // 上でScalaz 7.1.1を指定しているのでそれはOK
    "org.specs2" %% "specs2-core"       % Specs2Version % "test",
    "org.specs2" %% "specs2-mock"       % Specs2Version % "test",
    "org.specs2" %% "specs2-scalacheck" % Specs2Version % "test",
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
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/",
    resolvers += "Scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= LibraryDependencies
  )
  lazy val project = Project(
    "P2P2ch",
    file("."),
    settings =
      Defaults.defaultSettings ++
        playScalaSettings      ++
        scalariformSettings    ++
        projectSettings
  ) dependsOn (
    P2PScalaProto
    )
}