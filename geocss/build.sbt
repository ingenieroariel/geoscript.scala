name := "geocss"

organization := "org.geoscript"

version := "0.7.1"

scalaVersion := "2.9.0-1"

resolvers ++= Seq(
  "OSGeo" at "http://download.osgeo.org/webdav/geotools/",
  "OpenGeo" at "http://repo.opengeo.org/"
)

libraryDependencies ++= {
  val gtVersion = "2.7.0.1"
  Seq(
    "org.scala-tools.testing" %% "specs" % "[1.6,1.7)" % "test",
    "org.geotools" % "gt-main" % gtVersion,
    "org.geotools" % "gt-cql" % gtVersion
  )
}