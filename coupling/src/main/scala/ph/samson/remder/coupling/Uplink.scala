package ph.samson.remder.coupling

/**
  * Interface from probe to app.
  */
trait Uplink {
  def debug(msg: String): Unit
  def info(msg: String): Unit
  def scrolled(x: Int, y: Int): Unit
}
