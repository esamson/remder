import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._
import sbt.Keys._

object Dependencies {

  val akkaVersion = "2.5.11"
  val betterFilesVersion = "3.4.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  val betterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVersion

  val commonmarkVersion = "0.11.0"
  val commonmark = "com.atlassian.commonmark" % "commonmark" % commonmarkVersion

  val jcabiManifests = "com.jcabi" % "jcabi-manifests" % "1.1"

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0"

  val plantuml = "net.sourceforge.plantuml" % "plantuml" % "1.2018.2"

  val scalaFx = "org.scalafx" %% "scalafx" % "8.0.144-R12"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4"

  val appDeps = Def.setting(
    Seq(
      akkaActor,
      akkaSlf4j,
      betterFiles,
      commonmark,
      jcabiManifests,
      logbackClassic,
      plantuml,
      scalaFx,
      scalaLogging,
      scalaTest % Test
    )
  )

  val probeDeps = Def.setting(
    Seq[ModuleID](
      "org.scala-js" %%% "scalajs-dom" % "0.9.5"
    )
  )

  val crossDeps = Def.setting(
    Seq[ModuleID]()
  )
  def crossLibs(configuration: Configuration) =
    libraryDependencies ++= crossDeps.value.map(_ % configuration)
}
