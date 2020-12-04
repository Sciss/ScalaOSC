lazy val projectVersion = "1.2.3"

lazy val deps = new {
  val main = new {
    val scalaJavaLocales = "1.0.0"
  }
  val test = new {
    val scalaTest = "3.2.3"
  }
}

// sonatype plugin requires that these are in global
ThisBuild / version      := projectVersion
ThisBuild / organization := "de.sciss"


lazy val commonJvmSettings = Seq(
  crossScalaVersions    := Seq("3.0.0-M2", "2.13.4", "2.12.12"),
)

lazy val commonSettings = Seq(
  name                  := "ScalaOSC",
//  version               := projectVersion,
//  organization          := "de.sciss",
  scalaVersion          := "2.13.4",
  description           := "A library for OpenSoundControl (OSC), a message protocol used in multi-media applications.",
  homepage              := Some(url(s"https://git.iem.at/sciss/${name.value}")),
  licenses              := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  libraryDependencies += {
    "org.scalatest" %%% "scalatest" % deps.test.scalaTest % Test
  },
  scalacOptions       ++= Seq("-deprecation", "-unchecked", "-feature", "-Xsource:2.13", "-encoding", "utf8", "-Xlint:-stars-align,_"),
  scalacOptions in (Compile, compile) ++= {
    val jdkGt8 = scala.util.Properties.isJavaAtLeast("9")
    if (!isDotty.value && jdkGt8) Seq("-release", "8") else Nil // JDK >8 breaks API; skip scala-doc
  },
  sources in (Compile, doc) := {
    if (isDotty.value) Nil else (sources in (Compile, doc)).value // bug in dottydoc
  },
  initialCommands in console :=
    """import de.sciss.osc._
      |import Implicits._
      |""".stripMargin
)

lazy val root = crossProject(JVMPlatform, JSPlatform).in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(publishSettings)
  .settings(
    buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
      BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
    ),
    buildInfoPackage := "de.sciss.osc"
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-locales" % deps.main.scalaJavaLocales
    ),
    test := {},    // XXXSJS Scala.js has broken numeric type pattern matching
  )

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    val h = "git.iem.at"
    val a = s"sciss/${name.value}"
    Some(ScmInfo(url(s"https://$h/$a"), s"scm:git@$h:$a.git"))
  },
)

