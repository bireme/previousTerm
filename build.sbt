ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "previousTerm"
  )

val jakartaServletApiVersion = "6.0.0"
val jettyVersion = "11.0.13"

val jacksonVersion = "2.15.0" //"2.14.2"
val jsonSimpleVersion = "1.1.1"
val log4jVersion = "2.20.0" //"2.19.0"
val luceneVersion = "9.5.0"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.googlecode.json-simple" % "json-simple" % jsonSimpleVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-web" % log4jVersion,
  "org.apache.lucene" % "lucene-core" % luceneVersion,
  "org.apache.lucene" % "lucene-analysis-common" % luceneVersion,
  "org.apache.lucene" % "lucene-backward-codecs" % luceneVersion,
)

libraryDependencies += "jakarta.servlet" % "jakarta.servlet-api" % jakartaServletApiVersion

//Jetty / containerLibs := Seq("org.eclipse.jetty" % "jetty-runner" % jettyVersion)

enablePlugins(JettyPlugin)

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.first //MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

