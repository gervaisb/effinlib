package com.github.gervaisb.effinlib.recognition

import akka.actor.typed.{Behavior, PostStop, Signal, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, Routers}

private[recognition] object OcrRouter {
  def apply(factory: OcrFactory): Behavior[OcrNode.Command] =
    Behaviors.setup[OcrNode.Command](context => new OcrRouter(context, factory))
}

private[recognition] class OcrRouter(context: ActorContext[OcrNode.Command], factory: OcrFactory) extends AbstractBehavior[OcrNode.Command](context) {
  private val pool = Routers.pool(4) {
    println("Building behavior")
    Behaviors.supervise(OcrNode(factory))
      .onFailure(SupervisorStrategy.restart)
  }
  private val router = context.spawn(pool, "ocr-instances-pool")
  context.log.info(s"Ocr router started with 4 nodes")

  override def onMessage(msg: OcrNode.Command): Behavior[OcrNode.Command] = {
    // You may only perform this action upto maximum 500 number of times within 86400 seconds"
    router ! msg
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[OcrNode.Command]] = {
    case PostStop =>
      context.log.info("Ocr system stopped")
      this
  }

}
