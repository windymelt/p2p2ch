import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "P2P2ch"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "momijikawa" %% "p2pscalaproto" % "0.2.2",
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += Resolver.file(
      "local-ivy-repos", file(Path.userHome + "/.ivy2/local")
    )(Resolver.ivyStylePatterns)
  ).settings(projectSettings: _*) //.dependsOn(ensimePlugin)

  lazy val ensimePlugin = uri("https://github.com/aemoncannon/ensime-sbt-cmd.git")

}
