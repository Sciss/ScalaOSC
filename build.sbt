name                  := "ScalaOSC"
version               := "1.2.0-SNAPSHOT"
organization          := "de.sciss"
scalaVersion          := "2.13.0-M5"
crossScalaVersions    := Seq("2.12.8", "2.11.12", "2.13.0-M5")
description           := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications."
homepage              := Some(url(s"https://git.iem.at/sciss/${name.value}"))
licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

libraryDependencies += {
  val v = if (scalaVersion.value == "2.13.0-M5") "3.0.6-SNAP5" else "3.0.5"
  "org.scalatest" %% "scalatest" % v % Test
}

scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8")

initialCommands in console :=
  """import de.sciss.osc._
    |import Implicits._
    |""".stripMargin

// ---- build info ----

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
  BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
)

buildInfoPackage := "de.sciss.osc"

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

pomExtra := { val n = name.value
<scm>
  <url>git@git.iem.at:sciss/{n}.git</url>
  <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
