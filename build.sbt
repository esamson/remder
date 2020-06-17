import Dependencies._
import microsites._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Global settings
organization in ThisBuild := "ph.samson.remder"
scalaVersion in ThisBuild := "2.13.1"

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
      "-Ywarn-dead-code",
      "-Ywarn-unused:params"
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
  // https://tpolecat.github.io/2017/04/25/scalac-flags.html
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "UTF-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Ybackend-parallelism",
    s"${sys.runtime.availableProcessors()}", // maximum worker threads for backend
    "-Ybackend-worker-queue",
    s"${sys.runtime.availableProcessors() * 2}", // backend threads worker queue size
    "-Wdead-code", // Warn when dead code is identified.
    "-Wvalue-discard", // Warn when non-Unit expression results are unused.
    "-Wnumeric-widen", // Warn when numerics are widened.
    "-Woctal-literal", // Warn on obsolete octal syntax.
    "-Wunused:imports", // Warn if an import selector is not referenced.
    "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates", // Warn if a private member is unused.
    "-Wunused:locals", // Warn if a local definition is unused.
    "-Wunused:explicits", // Warn if an explicit parameter is unused.
    "-Wunused:implicits", // Warn if an implicit parameter is unused.
    "-Wextra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
    "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
    "-Xlint:valpattern", // Enable pattern checks in val definitions.
    "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
    "-Xlint:eta-sam", // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
    "-Xlint:deprecation" // Enable linted deprecations.
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
