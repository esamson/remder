package ph.samson.remder.app
import java.net.URL
import java.util.jar.Attributes.Name.{
  IMPLEMENTATION_TITLE,
  IMPLEMENTATION_VENDOR,
  IMPLEMENTATION_VERSION
}
import java.util.jar.Manifest

import scala.jdk.CollectionConverters._
import scala.util.Using

object Version {

  private val cl = getClass.getClassLoader

  private def read(url: URL) = {
    Using(url.openStream())(stream => new Manifest(stream)).get
  }

  val Version = {
    val versions = for {
      url <- cl.getResources("META-INF/MANIFEST.MF").asScala
      attributes = read(url).getMainAttributes
      if attributes.getValue(IMPLEMENTATION_VENDOR) == "ph.samson.remder" &&
        attributes.getValue(IMPLEMENTATION_TITLE) == "remder-app"
    } yield attributes.getValue(IMPLEMENTATION_VERSION)

    versions.toList.headOption.getOrElse("(dev)")
  }
}
