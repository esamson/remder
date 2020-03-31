package ph.samson.remder.app

import com.typesafe.scalalogging.StrictLogging
import scalafx.application.JFXApp

object Engine extends StrictLogging {

  def start(parameters: JFXApp.Parameters): Unit = {
    logger.info(s"starting: $parameters")
  }

}
