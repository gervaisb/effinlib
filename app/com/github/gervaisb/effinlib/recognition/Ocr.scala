package com.github.gervaisb.effinlib.recognition

import java.net.URL

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

private[recognition] trait Ocr {
  def recognize(image: URL)(implicit ec:ExecutionContext): Future[immutable.Set[String]]
}
