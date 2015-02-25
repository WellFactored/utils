name := "wiredthing-utils"

organization in ThisBuild := "com.wiredthing"

organizationName in ThisBuild := "WiredThing Ltd."

organizationHomepage in ThisBuild := Some(url("http://wiredthing.com"))

startYear in ThisBuild := Some(2014)

lazy val scalaUtils = project in file("scala-utils")

lazy val scalaUtilsPlay = (project in file("scala-utils-play")).dependsOn(scalaUtils)

lazy val status = project in file("play-status")

lazy val healthcheck = (project in file("play-healthcheck")).dependsOn(status)

scalaVersion in ThisBuild := "2.11.5"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-language:reflectiveCalls", "-language:postfixOps")

resolvers in ThisBuild += "WiredThing Internal Forks Repository" at "https://wiredthing.artifactoryonline.com/wiredthing/libs-forked-local"

publishTo in ThisBuild := {
  if (isSnapshot.value)
    Some("WiredThing Internal Snapshots Repository" at "https://wiredthing.artifactoryonline.com/wiredthing/libs-snapshots-local")
  else
    Some("WiredThing Internal Libraries Repository" at "https://wiredthing.artifactoryonline.com/wiredthing/libs-releases-local")
}

credentials in ThisBuild += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies in ThisBuild ++= Seq(
  "joda-time" % "joda-time" % "2.3" withSources(),
  "org.joda" % "joda-convert" % "1.2",
  "org.scalaz" %% "scalaz-core" % "7.1.0" withSources(),
  "org.scalatest" %% "scalatest" % "2.1.7" % "test"
)

shellPrompt in ThisBuild := { state: State => "utils " + Project.extract(state).currentRef.project + "> "}
