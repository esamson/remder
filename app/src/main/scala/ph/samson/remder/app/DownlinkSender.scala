package ph.samson.remder.app

import netscape.javascript.JSObject
import ph.samson.remder.coupling.Downlink
import scalafx.application.Platform

class DownlinkSender(downlink: JSObject) extends Downlink {
  override def scroll(x: Int, y: Int): Unit =
    Platform.runLater {
      downlink.call("scroll", Int.box(x), Int.box(y))
    }
}
