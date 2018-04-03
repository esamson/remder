package ph.samson.remder.app

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.scalalogging.Logger
import netscape.javascript.JSObject
import ph.samson.remder.coupling.Uplink
import scalafx.application.Platform
import scalafx.scene.web.WebEngine

import scala.io.Source

class Presenter(engine: WebEngine) extends Actor with ActorLogging with Uplink {
  import Presenter._

  var pageXOffset = 0
  var pageYOffset = 0

  override def receive: Receive = {
    case Present(html) =>
      Platform.runLater {
        engine.loadContent(html)
      }
    case Probe =>
      Platform.runLater {
        engine.executeScript("window") match {
          case window: JSObject =>
            window.setMember("uplink", this)
            engine.executeScript(probe)
            engine.executeScript("Downlink") match {
              case downlink: JSObject =>
                new DownlinkSender(downlink).scroll(pageXOffset, pageYOffset)
              case other => log.error(s"no downlink $other")
            }
        }
      }
  }

  val probeLogger = Logger("probe")

  override def debug(msg: String): Unit = probeLogger.debug(msg)

  override def info(msg: String): Unit = probeLogger.info(msg)

  override def scrolled(x: Int, y: Int): Unit = {
    pageXOffset = x
    pageYOffset = y
  }
}

object Presenter {
  case class Present(html: String)
  case object Probe

  def props(engine: WebEngine): Props = Props(new Presenter(engine))

  val probe: String = Source.fromResource("probe-opt.js").mkString
}
