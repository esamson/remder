package ph.samson.remder.coupling

/**
  * Interface from app to probe.
  */
trait Downlink {
  def scroll(x: Int, y: Int): Unit
}
