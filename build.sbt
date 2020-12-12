import Dependencies._
import microsites._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Global settings
organization in ThisBuild := "ph.samson.remder"
scalaVersion in ThisBuild := "2.13.4"

licenses in ThisBuild := Seq(
  "MIT" -> url("http://opensource.org/licenses/mit-license.php")
)
homepage in ThisBuild := Some(url("https://github.com/esamson/remder"))
developers in ThisBuild := List(
  Developer(
    id = "esamson",
    name = "Edward Samson",
    email = "edward@samson.ph",
    url = url("https://edward.samson.ph")
  )
)
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/esamson/remder"),
    "scm:git:git@github.com:esamson/remder.git"
  )
)

releaseEarlyWith in Global := SonatypePublisher

// Root project
name := "remder"
pgpPublicRing := file("./travis/pubring.asc")
pgpSecretRing := file("./travis/secring.asc")
sonatypeProfileName := "ph.samson"
aggregate in releaseEarly := false
publish / skip := true

lazy val app = subproject("remder-app", file("app"))
  .dependsOn(jvmCoupling)
  .settings(
    libraryDependencies ++= appDeps.value,
    crossLibs(Compile),
    resources in Compile += (fullOptJS in probe in Compile).value.data,
    fork in run := true,
    javaOptions in run ++= devRunOpts,
    javaOptions in reStart ++= devRunOpts
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

lazy val docs = subproject("docs")
  .enablePlugins(MicrositesPlugin)
  .settings(
    micrositeName := "Remder",
    micrositeDescription := "Remder: markdown rendered live",
    micrositeBaseUrl := "/remder",
    micrositeAuthor := "Edward Samson",
    micrositeHomepage := "https://esamson.github.io/remder/",
    micrositeOrganizationHomepage := "https://edward.samson.ph/",
    micrositeGithubOwner := "esamson",
    micrositeGithubRepo := "remder",
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositePushSiteWith := GitHub4s,
    micrositeAnalyticsToken := sys.env.getOrElse("ANALYTICS_TOKEN", ""),
    micrositeExtraMdFiles := Map(
      file("README.md") -> ExtraMdFileConfig(
        "index.md",
        "home",
        Map(
          "technologies" ->
            s"""|
                | - first: ["ScalaFX", "WebView and desktop GUI."]
                | - second: ["commonmark-java", "Markdown rendering."]
                | - third: ["PlantUML", "Diagram rendering."]
                |""".stripMargin
        )
      )
    ),
    micrositeFooterText := {
      for {
        gitDescribe <- dynverGitDescribeOutput.value
        default <- micrositeFooterText.value
      } yield {
        val versionLink = if (gitDescribe.isDirty()) {
          "https://github.com/esamson/remder/commits/master"
        } else if (gitDescribe.isSnapshot()) {
          s"https://github.com/esamson/remder/commit/${gitDescribe.commitSuffix.sha}"
        } else {
          s"https://github.com/esamson/remder/releases/tag/v${version.value}"
        }

        val versionFooter =
          s"""<p><a href="$versionLink">Remder ${version.value}</a></p>"""

        val micrositesFooter = if (default.startsWith("<p>")) {
          default.replace("<p>", """<p style="font-size: 50%">""")
        } else {
          default
        }

        s"""|$versionFooter
            |$micrositesFooter
            |""".stripMargin
      }
    }
  )

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
