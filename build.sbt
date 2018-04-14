import Dependencies._
import microsites._

// Global settings
organization in ThisBuild := "ph.samson.remder"
scalaVersion in ThisBuild := "2.12.5"

licenses in ThisBuild := Seq(
  "MIT" -> url("http://opensource.org/licenses/mit-license.php"))
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

lazy val coupling = crossProject
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
    micrositeDescription := "Remder: live markdown preview",
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
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xfuture", // Turn on future language features.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
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
