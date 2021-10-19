package com.github.gervaisb.effinlib.recognition

import akka.{ actor => classic }
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.Config
import play.api.libs.ws.StandaloneWSClient

trait RecognitionComponents {


  def recognition(config:Config, system: classic.ActorSystem, http:StandaloneWSClient):Recognition = {
    val factory:OcrFactory = () => {
      new WordsSanitizer(new OcrSpaceOcr(http, config))
    }
    new RecognitionService(system, factory)
  }
}
