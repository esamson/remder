package ph.samson.remder.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import better.files.File
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import ph.samson.remder.app.Presenter.Present
import ph.samson.remder.app.Renderer.DoRender

class Renderer(presenter: ActorRef) extends Actor with ActorLogging {

  def doRender(file: File): Unit = {
    log.debug(s"doRender($file)")
    val parser = Parser.builder().build()
    val document = file.fileReader(parser.parseReader)
    val renderer = HtmlRenderer.builder().build()
    val output = renderer.render(document)
    presenter ! Present(output)
  }

  override def receive: Receive = {
    case DoRender(file) => doRender(file)
  }
}

object Renderer {

  case class DoRender(markdown: File)

  def props(presenter: ActorRef): Props = Props(new Renderer(presenter))
}
