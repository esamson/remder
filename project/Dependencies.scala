import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._
import sbt.Keys._

object Dependencies {

  val akkaVersion = "2.5.15"
  val betterFilesVersion = "3.6.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  val betterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVersion

  val commonmarkVersion = "0.11.0"
  val commonmark = "com.atlassian.commonmark" % "commonmark" % commonmarkVersion
  val commonmarkExtGfmTables = "com.atlassian.commonmark" % "commonmark-ext-gfm-tables" % commonmarkVersion

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  val plantuml = "net.sourceforge.plantuml" % "plantuml" % "1.2018.11"

  val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"

  val scalaFx = "org.scalafx" %% "scalafx" % "8.0.144-R12"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  val appDeps = Def.setting(
    Seq(
      akkaActor,
      akkaSlf4j,
      betterFiles,
      commonmark,
      commonmarkExtGfmTables,
      logbackClassic,
      plantuml,
      scalaArm,
      scalaFx,
      scalaLogging,
      scalaTest % Test
    )
  )

  val probeDeps = Def.setting(
    Seq[ModuleID](
      "org.scala-js" %%% "scalajs-dom" % "0.9.6"
    )
  )

  val crossDeps = Def.setting(
    Seq[ModuleID]()
  )
  def crossLibs(configuration: Configuration) =
    libraryDependencies ++= crossDeps.value.map(_ % configuration)
}
