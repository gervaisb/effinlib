package com.github.gervaisb.effinlib.recognition

import com.github.gervaisb.effinlib.{Publication, Words}

import scala.concurrent.Future

trait Recognition {

  def recognize(publication: Publication): Future[Publication with Words]

}
