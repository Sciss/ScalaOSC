name := "scalaosc"

version := "0.24"

organization := "de.sciss"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.8.1", "2.9.0", "2.9.1")

// ---- publishing ----

publishTo := Some(ScalaToolsReleases)

pomExtra :=
<licenses>
  <license>
    <name>LGPL v2.1+</name>
    <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

