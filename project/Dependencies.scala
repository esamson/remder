import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._
import sbt.Keys._

object Dependencies {

  val akkaVersion = "2.6.15"
  val betterFilesVersion = "3.9.1"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  val betterFiles =
    "com.github.pathikrit" %% "better-files" % betterFilesVersion

  val commonmarkVersion = "0.18.0"
  val commonmark = "org.commonmark" % "commonmark" % commonmarkVersion
  val commonmarkExtGfmTables =
    "org.commonmark" % "commonmark-ext-gfm-tables" % commonmarkVersion

  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.4"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

  val plantuml = "net.sourceforge.plantuml" % "plantuml" % "1.2021.8"

  val javafxWeb = "org.openjfx" % "javafx-web" % "18-ea+1"
  val scalaFx = "org.scalafx" %% "scalafx" % "16.0.0-R24"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9"

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
      scalaFx,
      scalaLogging,
      scalaTest % Test
    )
  )

  val probeDeps = Def.setting(
    Seq[ModuleID](
      "org.scala-js" %%% "scalajs-dom" % "1.1.0"
    )
  )

  val crossDeps = Def.setting(
    Seq[ModuleID]()
  )
  def crossLibs(configuration: Configuration) =
    libraryDependencies ++= crossDeps.value.map(_ % configuration)
}
