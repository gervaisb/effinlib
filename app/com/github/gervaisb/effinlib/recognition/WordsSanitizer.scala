package com.github.gervaisb.effinlib.recognition
import java.net.URL

import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class WordsSanitizer(private val ocr: Ocr) extends Ocr {
  private val log = LoggerFactory.getLogger(this.toString)
  private val validWords = {
    val source = Source.fromURL(getClass.getResource("/Dictionary.txt"))
    val words = immutable.HashSet(source.getLines().map(_.toLowerCase).toSeq:_*)
    source.close()
    words
  }
  override def recognize(image: URL)(implicit ec: ExecutionContext): Future[immutable.Set[String]] = {
    ocr.recognize(image).map(sanitize)
  }

  private def sanitize(words:Set[String]):immutable.Set[String] = {
    words.map(clean)
      .filter(_.length>1)
      .filterNot(_.startsWith("@"))
      .filter(validWord)
  }

  private def clean(word:String):String = {
    word.toLowerCase()
      .replaceAll("[^\\w]","")
  }

  private def validWord(word:String):Boolean = {
    if ( validWords.contains(word) ) {
      true
    } else {
      log.info(s"Word '$word' ignored, not in dictionary.")
      false
    }
  }
}
