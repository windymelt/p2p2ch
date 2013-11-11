// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers ++= Seq(
Resolver.file("Local Repository", file("<projdir>/Play20/repository/local"))(Resolver.ivyStylePatterns),
"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.2")
