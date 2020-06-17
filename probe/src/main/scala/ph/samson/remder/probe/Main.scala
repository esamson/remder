package ph.samson.remder.probe

import org.scalajs.dom
import org.scalajs.dom.raw.UIEvent
import scalajs.js.timers

object Main {

  def main(args: Array[String]): Unit = {
    val window = dom.window
    def reportScroll() =
      timers.setTimeout(100) {
        Uplink.scrolled(window.pageXOffset.toInt, window.pageYOffset.toInt)
      }
    var handle = reportScroll()
    window.addEventListener[UIEvent](
      "scroll",
      (_: UIEvent) => {
        timers.clearTimeout(handle)
        handle = reportScroll()
      }
    )
    Uplink.debug("probe initialized")
  }
}
