import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._
import sbt.Keys._

object Dependencies {

  val akkaVersion = "2.5.31"
  val betterFilesVersion = "3.8.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  val betterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVersion

  val commonmarkVersion = "0.13.0"
  val commonmark = "com.atlassian.commonmark" % "commonmark" % commonmarkVersion
  val commonmarkExtGfmTables = "com.atlassian.commonmark" % "commonmark-ext-gfm-tables" % commonmarkVersion

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val plantuml = "net.sourceforge.plantuml" % "plantuml" % "1.2020.4"

  val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"

  val javafxWeb = "org.openjfx" % "javafx-web" % "12"
  val scalaFx = "org.scalafx" %% "scalafx" % "12.0.2-R18"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  val appDeps = Def.setting(
    Seq(
      akkaActor,
      akkaSlf4j,
      betterFiles,
      commonmark,
      commonmarkExtGfmTables,
      javafxWeb,
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
      "org.scala-js" %%% "scalajs-dom" % "0.9.7"
    )
  )

  val crossDeps = Def.setting(
    Seq[ModuleID]()
  )
  def crossLibs(configuration: Configuration) =
    libraryDependencies ++= crossDeps.value.map(_ % configuration)
}
