package ph.samson.remder.app

import java.awt.Desktop
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.{Executors, ScheduledFuture}

import akka.actor.ActorSystem
import better.files.{File, FileMonitor}
import com.typesafe.scalalogging.{Logger, StrictLogging}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.concurrent.Worker
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebErrorEvent.USER_DATA_DIRECTORY_ALREADY_IN_USE
import javafx.stage.WindowEvent
import ph.samson.remder.app.Presenter.Scroll
import ph.samson.remder.app.Renderer.{ToBrowser, ToViewer}
import ph.samson.remder.coupling.Uplink
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.property.IntegerProperty
import scalafx.scene.Scene
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.{BorderPane, Priority}
import scalafx.scene.paint.Color.Black
import scalafx.scene.web.WebView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.NonFatal

object Main extends JFXApp with Uplink with StrictLogging {

  private val markdownFile = {
    val f = parameters.unnamed.headOption.map(File(_))
    f.getOrElse(File.newTemporaryFile(suffix = ".tmp.md"))
  }

  require(markdownFile.isRegularFile && markdownFile.isReadable,
          s"Can't read $markdownFile")
  logger.info(s"remdering $markdownFile")
  sys.addShutdownHook({
    if (markdownFile.name.endsWith(".tmp.md") && markdownFile.isEmpty) {
      markdownFile.delete()
    }
  })

  if (markdownFile.name.endsWith(".tmp.md")) {
    try {
      Desktop.getDesktop.edit(markdownFile.toJava)
    } catch {
      case NonFatal(ex) => logger.debug(s"Can't open $markdownFile editor", ex)
    }
  }

  private val browser = new WebView {
    hgrow = Priority.Always
    vgrow = Priority.Always
  }

  private val engine = browser.engine

  private val system: ActorSystem = ActorSystem("remder")
  private val presenter = system.actorOf(Presenter.props(engine, this))
  private val renderer = system.actorOf(Renderer.props(presenter))

  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
  private def requestRender() = {
    scheduledExecutor.schedule(new Runnable {
      override def run(): Unit = renderer ! ToViewer(markdownFile)
    }, 100, MILLISECONDS)
  }
  private var sf: ScheduledFuture[_] = requestRender()

  if (sys.props("os.name") == "Mac OS X") {
    // WatchService is slow on macOS - use polling
    var latest = markdownFile.lastModifiedTime
    scheduledExecutor.scheduleWithFixedDelay(() => {
      val now = markdownFile.lastModifiedTime
      if (now.isAfter(latest)) {
        latest = now
        sf = requestRender()
      }
    }, 400, 400, MILLISECONDS)
  } else {
    val watcher = new FileMonitor(markdownFile, recursive = false) {
      override def onModify(file: File, count: Int): Unit = {
        sf.cancel(false)
        sf = requestRender()
      }
    }
    watcher.start()(ExecutionContext.global)
  }

  engine.onError = (event: WebErrorEvent) => {
    if (event.getEventType == USER_DATA_DIRECTORY_ALREADY_IN_USE) {
      engine.userDataDirectory = File.newTemporaryDirectory().toJava
    } else {
      logger.error(s"WebEngine error: ${event.getMessage}", event.getException)
    }
  }

  stage = new PrimaryStage {
    title = s"Remder ${Version.Version}: $markdownFile"
    scene = new Scene {
      fill = Black
      root = new BorderPane {
        hgrow = Priority.Always
        vgrow = Priority.Always
        center = browser
      }
    }

    onCloseRequest = (_: WindowEvent) => {
      scheduledExecutor.shutdown()
      Await.result(system.terminate(), Duration.Inf)
      Platform.exit()
      System.exit(0)
    }

    filterEvent(KeyEvent.KeyTyped) { (ke: KeyEvent) =>
      if (ke.character == "b") {
        ke.consume()
        renderer ! ToBrowser(markdownFile)
      }
    }
  }

  val xScroll = IntegerProperty(0)
  val yScroll = IntegerProperty(0)

  for (window <- WindowMemory.load(markdownFile)) {
    logger.debug(s"loaded $window")
    stage.x = window.x
    stage.y = window.y
    stage.width = window.width
    stage.height = window.height
    xScroll() = window.xScroll
    yScroll() = window.yScroll
  }

  engine.getLoadWorker
    .stateProperty()
    .addListener(new ChangeListener[Worker.State] {
      override def changed(observable: ObservableValue[_ <: Worker.State],
                           oldValue: Worker.State,
                           newValue: Worker.State): Unit = {
        newValue match {
          case Worker.State.SUCCEEDED =>
            presenter ! Scroll(xScroll(), yScroll())
          case Worker.State.FAILED => logger.error("Loading FAILED")
          case _                   => // ignore
        }
      }
    })

  WindowMemory.gc()

  sys.addShutdownHook {
    WindowMemory.save(markdownFile,
                      stage.getX,
                      stage.getY,
                      stage.getWidth,
                      stage.getHeight,
                      xScroll(),
                      yScroll())
  }

  val probeLogger = Logger("probe")

  override def debug(msg: String): Unit = probeLogger.debug(msg)

  override def info(msg: String): Unit = probeLogger.info(msg)

  override def scrolled(x: Int, y: Int): Unit = {
    xScroll() = x
    yScroll() = y
  }
}
