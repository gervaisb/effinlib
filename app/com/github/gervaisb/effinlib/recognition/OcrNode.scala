package com.github.gervaisb.effinlib.recognition

import java.net.URL

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import scala.collection.immutable
import scala.util.{Failure, Success}

private[recognition] object OcrNode {

  def apply(factory: OcrFactory): Behavior[Command] = {
    Behaviors.setup(context => new OcrNode(context, factory.apply()))
  }

  sealed trait Command

  final case class Recognize(messageId: Long, url: URL, replyTo: ActorRef[Response]) extends Command

  private final case class Reply(response: Response, to: ActorRef[Response]) extends Command

  sealed trait Response

  final case class RecognitionFinished(messageId: Long, words: immutable.Set[String]) extends Response

  final case class RecognitionFailed(messageId: Long, cause: Throwable) extends Response

}

private[recognition] class OcrNode(context: ActorContext[OcrNode.Command], ocr: Ocr) extends
  AbstractBehavior[OcrNode.Command](context) {

  import OcrNode._

  context.log.info(s"Ocr node started for $ocr")

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case Recognize(messageId, url, client) =>
      context.pipeToSelf(ocr.recognize(url)(context.executionContext)) {
        case Success(words) => Reply(RecognitionFinished(messageId, words), to = client)
        case Failure(cause) => Reply(RecognitionFailed(messageId, cause), to = client)
      }
      this

    case Reply(response, client) =>
      client ! response
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info(s"Ocr node actor for $ocr stopped")
      this
  }

}
