name         := "scalaosc"
version      := "0.33"
organization := "de.sciss"
scalaVersion := "2.11.8"
description  := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications."
homepage     := Some(url("https://github.com/Sciss/ScalaOSC"))
licenses     := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "2.1.3" % "test")

scalacOptions ++= Seq("-deprecation", "-unchecked")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
   Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )

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

