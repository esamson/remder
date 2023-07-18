import Dependencies._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Global settings
ThisBuild / organization := "ph.samson.remder"
ThisBuild / scalaVersion := "2.13.11"

ThisBuild / licenses := Seq(
  "MIT" -> url("http://opensource.org/licenses/mit-license.php")
)
ThisBuild / homepage := Some(url("https://github.com/esamson/remder"))
ThisBuild / developers := List(
  Developer(
    id = "esamson",
    name = "Edward Samson",
    email = "edward@samson.ph",
    url = url("https://edward.samson.ph")
  )
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/esamson/remder"),
    "scm:git:git@github.com:esamson/remder.git"
  )
)

// Root project
name := "remder"
publish / skip := true

lazy val app = subproject("remder-app", file("app"))
  .dependsOn(jvmCoupling)
  .settings(
    libraryDependencies ++= appDeps.value,
    crossLibs(Compile),
    Compile / resources += (probe / Compile / fullOptJS).value.data,
    run / fork := true,
    run / javaOptions ++= devRunOpts
  )
  .enablePlugins(JavaAppPackaging)

lazy val devRunOpts = Seq(
  "-DREMDER_LOG=debug"
)

lazy val probe = subproject("probe")
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jsCoupling)
  .settings(
    libraryDependencies ++= probeDeps.value,
    crossLibs(Compile),
    scalaJSUseMainModuleInitializer := true,
    scalacOptions --= Seq(
      // these don's play well with js.native facades
      "-Wdead-code"
    )
  )

lazy val coupling = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("coupling"))
  .settings(
    baseSettings,
    crossLibs(Provided)
  )

lazy val jvmCoupling = coupling.jvm
lazy val jsCoupling = coupling.js

def subproject(name: String, dir: File): Project =
  Project(name, dir).settings(baseSettings)
def subproject(name: String): Project = subproject(name, file(name))

lazy val baseSettings = Seq(
  scalacOptions ++= Seq(
    "-Ybackend-parallelism",
    s"${sys.runtime.availableProcessors()}", // maximum worker threads for backend
    "-Ybackend-worker-queue",
    s"${sys.runtime.availableProcessors() * 2}", // backend threads worker queue size
    "-Woctal-literal", // Warn on obsolete octal syntax.
    "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
    "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
    "-Xlint:valpattern", // Enable pattern checks in val definitions.
    "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
    "-Xlint:eta-sam" // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
  ),
  javacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-Werror",
    "-Xlint:all",
    "-Xlint:-serial"
  )
)

addCommandAlias("makeZip", "show universal:packageBin")
