package com.github.gervaisb.effinlib.api

import com.github.gervaisb.effinlib.{Publication, Words}
import com.github.gervaisb.effinlib.index.Index
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}


class ApiController @Inject()(val index: Index, val cc:ControllerComponents) extends AbstractController(cc) {
  implicit val ec:ExecutionContext = cc.executionContext

  def lookup(terms:String) = Action.async {
    val fs = terms.split(",").map(word => index.search(word))
    val ps = Future.sequence(fs.toSeq).map(_.flatten)
    ps.map { pubs => Json.obj(
      "lookup" -> Json.obj(
        "terms" -> terms.split(","),
        "result" -> pubs.map { pub => Json.obj(
          "words" -> pub.words.toSeq.sorted,
          "popularity" -> pub.popularity,
          "image" -> pub.url.toExternalForm,
        )}
      )
    )}.map(Ok(_))
  }

}
