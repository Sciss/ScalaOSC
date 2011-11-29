name := "scalaosc"

version := "0.33-SNAPSHOT"

organization := "de.sciss"

scalaVersion := "2.9.1"

description := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications."

homepage := Some( url( "https://github.com/Sciss/ScalaOSC" ))

licenses := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))

libraryDependencies ++= Seq( "org.scalatest" %% "scalatest" % "1.6.1" % "test" )

retrieveManaged := true

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

// ---- publishing ----

publishTo <<= version { (v: String) =>
   Some( "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/".+(
      if( v.endsWith( "-SNAPSHOT")) "snapshots/" else "releases/"
   ))
}

pomExtra :=
<licenses>
  <license>
    <name>LGPL v2.1+</name>
    <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>

credentials += Credentials( Path.userHome / ".ivy2" / ".credentials" )

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "osc", "open-sound-control", "sound", "network" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "ScalaOSC" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))
