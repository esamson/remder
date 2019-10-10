package ph.samson.remder.app

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  ObjectInputStream,
  ObjectOutputStream
}
import java.time.{Instant, ZonedDateTime}
import java.util.prefs.Preferences

import better.files.File
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future
import scala.util.{Failure, Success, Try, Using}

/**
  * Remember last window coordinates and dimensions for next time.
  */
object WindowMemory extends StrictLogging {

  private val DefaultKey = classOf[Window].getName
  private val DefaultBytes = Array.emptyByteArray
  private val prefs = Preferences.userNodeForPackage(getClass)

  def fileKey(markdownFile: File): String =
    markdownFile.path.toAbsolutePath.toString.hashCode.toString

  def save(
      markdownFile: File,
      x: Double,
      y: Double,
      width: Double,
      height: Double,
      xScroll: Int,
      yScroll: Int
  ): Unit = {

    val serialize = Using.Manager { use =>
      val buffer = use(new ByteArrayOutputStream())
      val out = use(new ObjectOutputStream(buffer))

      out.writeObject(
        Window(x, y, width, height, xScroll, yScroll, Instant.now())
      )
      out.flush()
      buffer.toByteArray
    }

    val puts = for {
      bytes <- serialize
    } yield {
      // save window memory for this file
      prefs.putByteArray(fileKey(markdownFile), bytes)

      // use same window memory for next file with no previous memory
      prefs.putByteArray(DefaultKey, bytes)

      prefs.flush()
    }

    logger.debug(s"save: $puts")
  }

  def load(markdownFile: File): Option[Window] = {
    val savedBytes = {
      val fileBytes = prefs.getByteArray(fileKey(markdownFile), DefaultBytes)
      if (fileBytes.nonEmpty) {
        Some(fileBytes)
      } else {
        val defaultBytes = prefs.getByteArray(DefaultKey, DefaultBytes)
        if (defaultBytes.nonEmpty) {
          Some(defaultBytes)
        } else {
          None
        }
      }
    }

    savedBytes.flatMap(
      bytes =>
        Window(bytes) match {
          case Success(w) => Some(w)
          case Failure(ex) =>
            logger.debug(s"load failed", ex)
            None
        }
    )
  }

  /**
    * Asynchronously clean up window memory
    */
  def gc(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val GcThreshold = ZonedDateTime.now().minusMonths(1)
      for {
        key <- prefs.keys()
        if key != DefaultKey
        markdownFile = File(key)
      } {
        if (!markdownFile.isReadable) {
          logger.debug(s"removing missing $markdownFile")
          prefs.remove(key)
        } else {
          Window(prefs.getByteArray(key, DefaultBytes)) match {
            case Success(w) =>
              if (ZonedDateTime
                    .from(w.lastSave)
                    .isBefore(GcThreshold)) {
                logger.debug(s"removing old $w")
                prefs.remove(key)
              }
            case Failure(ex) =>
              logger.debug("removing bad memory", ex)
              prefs.remove(key)
          }
        }
      }
    }

    ()
  }

  case class Window(
      x: Double,
      y: Double,
      width: Double,
      height: Double,
      xScroll: Int,
      yScroll: Int,
      lastSave: Instant
  )

  object Window {
    def apply(bytes: Array[Byte]): Try[Window] = {
      val read = Using.Manager { use =>
        val buffer = use(new ByteArrayInputStream(bytes))
        val in = use(new ObjectInputStream(buffer))
        in.readObject()
      }

      read
        .map({
          case w: Window => w
          case other =>
            throw new IllegalArgumentException(
              s"Can't deserialize to Window $other"
            )
        })
    }
  }

}
