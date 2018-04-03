package ph.samson.remder.app

import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.{Executors, ScheduledFuture}

import akka.actor.ActorSystem
import better.files.{File, FileMonitor}
import com.typesafe.scalalogging.StrictLogging
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.concurrent.Worker
import javafx.scene.web.WebErrorEvent
import javafx.stage.WindowEvent
import ph.samson.remder.app.Presenter.Probe
import ph.samson.remder.app.Renderer.DoRender
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, Priority}
import scalafx.scene.paint.Color.Black
import scalafx.scene.web.WebView

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends JFXApp with StrictLogging {

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

  private val browser = new WebView {
    hgrow = Priority.Always
    vgrow = Priority.Always
  }

  private val engine = browser.engine

  private val system: ActorSystem = ActorSystem("remder")
  private val presenter = system.actorOf(Presenter.props(engine))
  private val renderer = system.actorOf(Renderer.props(presenter))

  private val debounceExecutor = Executors.newSingleThreadScheduledExecutor()
  private def requestRender() = {
    debounceExecutor.schedule(new Runnable {
      override def run(): Unit = renderer ! DoRender(markdownFile)
    }, 100, MILLISECONDS)
  }
  private var sf: ScheduledFuture[_] = requestRender()
  private val watcher = new FileMonitor(markdownFile, recursive = false) {
    override def onModify(file: File, count: Int): Unit = {
      sf.cancel(false)
      sf = requestRender()
    }
  }
  watcher.start()(ExecutionContext.global)

  engine.onError = (event: WebErrorEvent) => {
    logger.error(s"onError: $event")
  }

  engine.getLoadWorker
    .stateProperty()
    .addListener(new ChangeListener[Worker.State] {
      override def changed(observable: ObservableValue[_ <: Worker.State],
                           oldValue: Worker.State,
                           newValue: Worker.State): Unit = {
        newValue match {
          case Worker.State.SUCCEEDED => presenter ! Probe
          case Worker.State.FAILED    => logger.error("Loading FAILED")
          case _                      => // ignore
        }
      }
    })

  stage = new PrimaryStage {
    title = "Remder"
    width = 1100
    height = 700
    scene = new Scene {
      fill = Black
      root = new BorderPane {
        hgrow = Priority.Always
        vgrow = Priority.Always
        center = browser
      }
    }

    onCloseRequest = (_: WindowEvent) => {
      debounceExecutor.shutdown()
      watcher.stop()
      Await.result(system.terminate(), Duration.Inf)
      Platform.exit()
    }
  }
}
