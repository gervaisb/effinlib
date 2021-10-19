package com.github.gervaisb.effinlib.recognition

import java.net.URL

import akka.{actor => classic}
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import com.github.gervaisb.effinlib.{Publication, Words}
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

private class RecognitionService(system: classic.ActorSystem, factory:OcrFactory) extends Recognition {
  import akka.actor.typed.scaladsl.adapter._
  private val log = LoggerFactory.getLogger(getClass)

  private implicit val timeout:Timeout = 10.seconds
  private implicit val ec:ExecutionContext = system.dispatcher
  private implicit val scheduler:Scheduler = system.toTyped.scheduler
  private val router = system.spawn(OcrRouter(factory), "ocr-system")

  override def recognize(publication: Publication): Future[Publication with Words] = {
    import akka.actor.typed.scaladsl.AskPattern._

    val result = router.ask[OcrNode.Response](ref =>
          OcrNode.Recognize(System.currentTimeMillis(), publication.url, ref) )
    result.map {
      case OcrNode.RecognitionFinished(_, words) =>
        log.debug(s"Recognition of $publication finished; [${words.mkString(", ")}].")
        RecognizedPublication(publication, words)
      case OcrNode.RecognitionFailed(_, cause) =>
        log.warn(s"Recognition of $publication failed : $cause.")
        throw cause
    }
  }


  private case class RecognizedPublication(origin: Publication, words:immutable.Set[String]) extends
    Publication(origin.url, origin.dateTime, origin.popularity) with Words
}
