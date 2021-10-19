package com.github.gervaisb.effinlib.recognition

import java.net.{URL, URLEncoder}

import com.typesafe.config.Config
import play.api.libs.json.JsValue
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * An Ocr implementation that use the Free API provided by <a href="https://ocr.space/">
 * https://ocr.space/</a>
 *
 * @param http An Http client to use
 * @param config The configuration that contains ""ocr.space.api.key"
 */
// https://ocr.space/OCRAPI
private[recognition] class OcrSpaceOcr(http:StandaloneWSClient, config:Config) extends Ocr {

  private val endpoint = "https://api.ocr.space/parse/imageurl"
  private lazy val options = Map(
    "apikey" -> config.getString("ocr.space.api.key"),
    "filetype" -> "JPG",
    "language" -> "eng",
    "scale" -> "true",
    "OCREngine" -> 2
  )

  override def recognize(image: URL)(implicit ec:ExecutionContext): Future[immutable.Set[String]] = {
    val params = (options + ("url" -> image.toExternalForm))
        .mapValues(_.toString)
        .toSeq

    for {
      response <- http.url(endpoint)
        .withQueryStringParameters(params:_*)
        .get()
    } yield {
      if (response.status>=200 && response.status<400 && (response.body[JsValue] \ "OCRExitCode").as[Int]==1 ) {
        val results = (response.body[JsValue]. \ ("ParsedResults")).as[Seq[JsValue]]
        val texts = results.map(result => (result \ "ParsedText").as[String] )
        immutable.Set(texts.flatMap(_.split("\\s")):_*)
      } else {
        val maybeMessage = (response.body[JsValue] \ ("ErrorMessage")).asOpt[String]
        val maybeDetails = (response.body[JsValue] \ ("ErrorDetails")).asOpt[String]
            .map(d => s" : $d")
        val details = maybeMessage.map(message => message+maybeDetails.get)
        throw new OcrSpaceFailure(response.status, response.statusText, details)
      }
    }

  }

}
