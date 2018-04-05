package ph.samson.remder.app
import java.net.URL
import java.util.jar.Attributes.Name.{
  IMPLEMENTATION_TITLE,
  IMPLEMENTATION_VENDOR,
  IMPLEMENTATION_VERSION
}
import java.util.jar.Manifest

import resource._

import scala.collection.JavaConverters._

object Version {

  private val cl = getClass.getClassLoader

  private def read(url: URL) = {
    managed(url.openStream()).acquireAndGet(stream => new Manifest(stream))
  }

  val Version = {
    val versions = for {
      url <- cl.getResources("META-INF/MANIFEST.MF").asScala
      attributes = read(url).getMainAttributes
      if attributes.getValue(IMPLEMENTATION_VENDOR) == "ph.samson.remder" &&
        attributes.getValue(IMPLEMENTATION_TITLE) == "remder-app"
    } yield attributes.getValue(IMPLEMENTATION_VERSION)

    versions.toList.head
  }
}
