import play.Project._

name := "twitter"

organization := "in.bharathwrites"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "oauth.signpost"            % "signpost-core"           % "1.2",
  "oauth.signpost"            % "signpost-commonshttp4"   % "1.2",
  "org.apache.httpcomponents" % "httpclient"              % "4.3.2",
  "org.apache.commons"        % "commons-io"              % "1.3.2",
  "org.apache.httpcomponents" % "fluent-hc"               % "4.3.2",
  "com.typesafe.akka"         %% "akka-actor"             % "2.2.3",
  "com.typesafe.akka"         %% "akka-slf4j"             % "2.2.3",
  "io.spray"                  % "spray-can"               % "1.2.0",
  "io.spray"                  % "spray-client"            % "1.2.0",
  "io.spray"                  % "spray-routing"           % "1.2.0",
  "io.spray"                  %% "spray-json"             % "1.2.5"
)

playScalaSettings

exportJars := true