package com.github.gervaisb.effinlib.collector

import java.net.URL
import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import com.github.gervaisb.effinlib
import com.github.gervaisb.effinlib.Publication
import com.tumblr.jumblr.JumblrClient
import com.tumblr.jumblr.types.PhotoPost
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConverters._
import scala.collection.immutable

class TumblrCollector(config: Config) extends Collector {
  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val log = LoggerFactory.getLogger(getClass)
  private val tumblr = {
    val client = new JumblrClient(
      config.getString("tumblr.api.consumer.key"),
      config.getString("tumblr.api.consumer.secret")
    )
    client.setToken(
      config.getString("tumblr.api.oauth.token"),
      config.getString("tumblr.api.oauth.secret")
    )
    client.blogInfo("effinbirds.tumblr.com")
  }

  private val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
  private val options = Map[String, Any](
    "type" -> "photo",
    "limit" -> 20,
    "offset" -> 0
  )
  private var page = 0

  override def next(): immutable.Seq[Publication] = {
    log.debug("Retrieving next page ({}).", page)
    val limit = options("limit").asInstanceOf[Int]
    val offset = (limit + page)
    val posts = tumblr.posts((options + ("offset" -> offset)).asJava)
    page += 1
    val pubs = posts.asScala
      .collect{ case post:PhotoPost => post }
      .flatMap { post =>
        val dateTime = ZonedDateTime.parse(post.getDateGMT, pattern).toOffsetDateTime
          .withOffsetSameInstant(ZoneOffset.UTC)
        val popularity = Some(post.getNoteCount.toInt)
        post.getPhotos.asScala.map { photo =>
          val size = photo.getOriginalSize
          new Publication(new URL(size.getUrl), dateTime, popularity)
        }
      }
    page = if ( posts.size()==limit) {
      page +1
    } else {
      log.info(s"End of posts reached at page $page, restarting from beginning.")
      0
    }
    immutable.Seq(pubs:_*)
  }
}
