package ph.samson.remder.app

import akka.actor.ActorSystem
import better.files.{File, FileMonitor}
import com.typesafe.scalalogging.{Logger, StrictLogging}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.concurrent.Worker
import javafx.event.{Event, EventHandler, EventType}
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebErrorEvent.USER_DATA_DIRECTORY_ALREADY_IN_USE
import javafx.stage.WindowEvent
import ph.samson.remder.app.Presenter.Scroll
import ph.samson.remder.app.Renderer.{ToBrowser, ToViewer}
import ph.samson.remder.app.WindowMemory.Window
import ph.samson.remder.coupling.Uplink
import scalafx.Includes._
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.beans.property.IntegerProperty
import scalafx.scene.Scene
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.{BorderPane, Priority}
import scalafx.scene.paint.Color.Black
import scalafx.scene.web.WebView
import scalafx.stage.Screen

import java.awt.Desktop
import java.time.Instant
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.{Executors, ScheduledFuture}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.NonFatal

object Main extends JFXApp3 with Uplink with StrictLogging {

  val probeLogger = Logger("probe")

  val xScroll = IntegerProperty(0)
  val yScroll = IntegerProperty(0)

  override def debug(msg: String): Unit = probeLogger.debug(msg)

  override def info(msg: String): Unit = probeLogger.info(msg)

  override def scrolled(x: Int, y: Int): Unit = {
    xScroll() = x
    yScroll() = y
  }

  override def start(): Unit = {

    val markdownFile = {
      val f = parameters.unnamed.headOption.map(File(_))
      f.getOrElse(File.newTemporaryFile(suffix = ".tmp.md"))
    }

    require(
      markdownFile.isRegularFile && markdownFile.isReadable,
      s"Can't read $markdownFile"
    )
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
        case NonFatal(ex) =>
          logger.debug(s"Can't open $markdownFile editor", ex)
      }
    }

    val browser = new WebView {
      hgrow = Priority.Always
      vgrow = Priority.Always
    }

    val engine = browser.engine

    val system: ActorSystem = ActorSystem("remder")
    val presenter = system.actorOf(Presenter.props(engine, this))
    val renderer = system.actorOf(Renderer.props(presenter))

    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    def requestRender() = {
      scheduledExecutor.schedule(
        new Runnable {
          override def run(): Unit = renderer ! ToViewer(markdownFile)
        },
        100,
        MILLISECONDS
      )
    }

    var sf: ScheduledFuture[_] = requestRender()

    if (sys.props("os.name") == "Mac OS X") {
      // WatchService is slow on macOS - use polling
      var latest = markdownFile.lastModifiedTime
      scheduledExecutor.scheduleWithFixedDelay(
        () => {
          val now = markdownFile.lastModifiedTime
          if (now.isAfter(latest)) {
            latest = now
            sf = requestRender()
          }
        },
        400,
        400,
        MILLISECONDS
      )
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
        logger.error(
          s"WebEngine error: ${event.getMessage}",
          event.getException
        )
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
          logger.debug("rendering to browser")
          renderer ! ToBrowser(markdownFile)
        }
      }

      override def addEventHandler[E <: Event](
          eventType: EventType[E],
          eventHandler: EventHandler[_ >: E]
      ): Unit = delegate.addEventHandler(eventType, eventHandler)

      override def removeEventHandler[E <: Event](
          eventType: EventType[E],
          eventHandler: EventHandler[_ >: E]
      ): Unit = delegate.removeEventHandler(eventType, eventHandler)

      override def addEventFilter[E <: Event](
          eventType: EventType[E],
          eventHandler: EventHandler[_ >: E]
      ): Unit = delegate.addEventFilter(eventType, eventHandler)

      override def removeEventFilter[E <: Event](
          eventType: EventType[E],
          eventHandler: EventHandler[_ >: E]
      ): Unit = delegate.removeEventFilter(eventType, eventHandler)
    }

    for (window <- WindowMemory.load(markdownFile)) {
      logger.debug(s"loaded $window")
      val screen: Screen = Screen
        .screensForRectangle(window.x, window.y, window.width, window.height)
        .headOption
        .map(new Screen(_))
        .getOrElse(Screen.primary)

      val bounds = screen.visualBounds
      logger.debug {
        val w = Window(
          bounds.maxX,
          bounds.maxY,
          bounds.width,
          bounds.height,
          0,
          0,
          Instant.now
        )
        s"Checking bounds: $w"
      }
      val width = if (window.width > 0 && window.width < bounds.width) {
        window.width
      } else {
        bounds.width / 2
      }
      val height = if (window.height > 0 && window.height < bounds.height) {
        window.height
      } else {
        bounds.height / 2
      }
      val x = if (window.x + width < bounds.maxX) {
        window.x
      } else {
        (bounds.width - width) / 2
      }
      val y = if (window.y + height < bounds.maxY) {
        window.y
      } else {
        (bounds.height - height) / 2
      }

      logger.debug {
        val w = Window(
          x,
          y,
          width,
          height,
          window.xScroll,
          window.yScroll,
          window.lastSave
        )
        s"Sizing: $w"
      }
      stage.x = x
      stage.y = y
      stage.width = width
      stage.height = height
      xScroll() = window.xScroll
      yScroll() = window.yScroll
    }

    engine.getLoadWorker
      .stateProperty()
      .addListener(new ChangeListener[Worker.State] {
        override def changed(
            observable: ObservableValue[_ <: Worker.State],
            oldValue: Worker.State,
            newValue: Worker.State
        ): Unit = {
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
      WindowMemory.save(
        markdownFile,
        stage.getX,
        stage.getY,
        stage.getWidth,
        stage.getHeight,
        xScroll(),
        yScroll()
      )
    }: Unit

  }
}
