package ph.samson.remder.probe

import ph.samson.remder.coupling

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object UplinkSender extends js.Object {
  val uplink: JsUplink = js.native
}

@js.native
trait JsUplink extends js.Object {
  def debug(@unused msg: String): Unit = js.native
  def info(@unused msg: String): Unit = js.native
  def scrolled(@unused x: Int, @unused y: Int): Unit = js.native
}

object Uplink extends coupling.Uplink {
  override def debug(msg: String): Unit = UplinkSender.uplink.debug(msg)

  override def info(msg: String): Unit = UplinkSender.uplink.info(msg)

  override def scrolled(x: Int, y: Int): Unit =
    UplinkSender.uplink.scrolled(x, y)
}
