package ph.samson.remder.app

import akka.actor.{Actor, ActorLogging, Props}
import netscape.javascript.JSObject
import ph.samson.remder.coupling.Uplink
import scalafx.application.Platform
import scalafx.scene.web.WebEngine

import scala.io.Source

class Presenter(engine: WebEngine, uplink: Uplink)
    extends Actor
    with ActorLogging {
  import Presenter._

  override def receive: Receive = {
    case Present(html) =>
      Platform.runLater {
        engine.loadContent(html)
      }
    case Scroll(x, y) =>
      Platform.runLater {
        engine.executeScript("window") match {
          case window: JSObject =>
            window.setMember("uplink", uplink)
            engine.executeScript(probe)
            engine.executeScript("Downlink") match {
              case downlink: JSObject =>
                new DownlinkSender(downlink).scroll(x, y)
              case other => log.error(s"no downlink $other")
            }
          case other =>
            throw new AssertionError(s"Expected window but got $other")
        }
      }
  }
}

object Presenter {
  case class Present(html: String)
  case class Scroll(x: Int, y: Int)

  def props(engine: WebEngine, uplink: Uplink): Props =
    Props(new Presenter(engine, uplink))

  val probe: String = Source.fromResource("probe-opt.js").mkString
}
