name := "scalaosc"

version := "1.0.1"

organization := "de.sciss"

scalaVersion := "2.9.2"

description := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications."

homepage := Some( url( "https://github.com/Sciss/ScalaOSC" ))

licenses := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/groups/public"

libraryDependencies in ThisBuild <+= scalaVersion { sv =>
   val v = sv match {
      case "2.10.0-RC3" => "1.8-B1"
      case "2.10.0-RC5" => "1.8-B1"
      case _            => "1.8"
   }
   "org.scalatest" %% "scalatest" % v % "test"
}

retrieveManaged := true

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

initialCommands in console := """import de.sciss.osc._
import Implicits._"""

// ---- build info ----

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq( name, organization, version, scalaVersion, description,
   BuildInfoKey.map( homepage ) { case (k, opt) => k -> opt.get },
   BuildInfoKey.map( licenses ) { case (_, Seq( (lic, _) )) => "license" -> lic }
)

buildInfoPackage := "de.sciss.osc"

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT" ))
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
<scm>
  <url>git@github.com:Sciss/ScalaOSC.git</url>
  <connection>scm:git:git@github.com:Sciss/ScalaOSC.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "osc", "open-sound-control", "sound", "network" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "ScalaOSC" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))
