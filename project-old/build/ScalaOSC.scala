import sbt._
import java.io.{ IOException, RandomAccessFile }

class ScalaOSCProject( info: ProjectInfo ) extends DefaultProject( info ) {
   // ---- publishing ----

   override def managedStyle  = ManagedStyle.Maven
   val publishTo              = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"

   override def packageDocsJar= defaultJarPath( "-javadoc.jar" )
   override def packageSrcJar = defaultJarPath( "-sources.jar" )
   val sourceArtifact         = Artifact.sources( artifactID )
   val docsArtifact           = Artifact.javadoc( artifactID )
   override def packageToPublishActions = super.packageToPublishActions ++ Seq( packageDocs, packageSrc )

   override def pomExtra =
      <licenses>
        <license>
          <name>LGPL v2.1+</name>
          <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>

   Credentials( Path.userHome / ".ivy2" / ".credentials", log )
}