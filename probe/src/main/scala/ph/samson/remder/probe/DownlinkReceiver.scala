package ph.samson.remder.probe

import ph.samson.remder.coupling.Downlink

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom

@JSExportTopLevel("Downlink")
object DownlinkReceiver extends Downlink {
  @JSExport
  override def scroll(x: Int, y: Int): Unit = {
    dom.window.scroll(x, y)
  }
}
