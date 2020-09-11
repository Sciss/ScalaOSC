lazy val commonSettings = Seq(
  name                  := "ScalaOSC",
  version               := "1.2.2",
  organization          := "de.sciss",
  scalaVersion          := "2.13.3",
  crossScalaVersions    := Seq("0.27.0-RC1", "2.13.3", "2.12.12"),
  description           := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications.",
  homepage              := Some(url(s"https://git.iem.at/sciss/${name.value}")),
  licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  libraryDependencies += {
    "org.scalatest" %% "scalatest" % "3.2.2" % Test
  },
  scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xsource:2.13", "-encoding", "utf8", "-Xlint:-stars-align,_"),
  scalacOptions in (Compile, compile) ++= {
    val jdkGt8  = scala.util.Properties.isJavaAtLeast("9")
    val isDotty = scalaVersion.value.startsWith("0.")  // https://github.com/lampepfl/dotty/issues/8634 
    if (!isDotty && jdkGt8) Seq("-release", "8") else Nil // JDK >8 breaks API; skip scala-doc
  },
  sources in (Compile, doc) := {
    val isDotty = scalaVersion.value.startsWith("0.")  // https://github.com/lampepfl/dotty/issues/8634 
    if (isDotty) Nil else (sources in (Compile, doc)).value // bug in dottydoc
  },
  initialCommands in console :=
    """import de.sciss.osc._
      |import Implicits._
      |""".stripMargin
)

lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
      BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
    ),
    buildInfoPackage := "de.sciss.osc"
  )

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
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
)
