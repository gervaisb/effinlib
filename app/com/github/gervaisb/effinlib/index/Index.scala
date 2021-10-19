package com.github.gervaisb.effinlib.index

import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import com.github.gervaisb.effinlib.{Publication, Words}
import com.redis.RedisClientPool
import org.slf4j.LoggerFactory

import scala.collection.{immutable, mutable}
import scala.concurrent.{ExecutionContext, Future}

class Index(private val redis: RedisClientPool) {

  type Entry = Publication with Words
  type Key = String
  object Key {
    def apply(entry: Publication):Key = {
      entry.url.getFile.replace("\\.jpg", "")
    }
  }

  private val log = LoggerFactory.getLogger(getClass)

  def search(word:String)(implicit ec:ExecutionContext):Future[immutable.Set[Entry]] = Future {
    redis.withClient { client =>
      client.smembers(word).map(restoreAll)
    }.getOrElse(immutable.Set.empty[Entry])
  }

  private def restoreAll(keys:Set[Option[Key]]):immutable.Set[Entry] = {
    keys.collect {
      case Some(key) => restore(key).get
    }
  }

  private def restore(key: Key):Option[Entry] = {
    redis.withClient { client =>
      client.hmget(key, "url", "dateTime", "popularity", "words").map { values:Map[String, String] =>
        val restored = new Publication(new URL(values("url")), OffsetDateTime.parse(values("dateTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME), Some(values("popularity").toInt)) with Words {
          override def words: Set[String] = values("words").split(",").toSet
        }
        log.trace(s"Restored entry [$key] : $restored")
        restored
      }
    }
  }

  private def store(entry: Entry):Key =  {
    redis.withClient { client =>
      val key = Key(entry)
      client.hmset(key,
        ("url" -> entry.url.toExternalForm) ::
        ("dateTime" -> entry.dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) ::
        ("popularity" -> entry.popularity.getOrElse(0)) ::
        ("words" -> entry.words.mkString(","))
        :: Nil
      )
      log.trace(s"Entry $entry stored with key [$key]")
      key
    }
  }

  def contains(publication: Publication): Boolean = {
    redis.withClient { client =>
      val key = Key(publication)
      client.hexists(key, "url")
    }
  }

  def append(publication: Publication with Words): Unit = {
    redis.withClient { client =>
      val key = store(publication)
      publication.words.foreach { word =>
        client.sadd(word, key)
      }
    }
    log.info(s"$publication added in index for ${publication.words}.")
  }

  def stats(): Stats = ???

  case class Stats(numberOfWords: Long, numberOfPublications: Long,
                   mostUsedWord: (String, Int), lessUsedWord: (String, Int),
                   mostPopularPublication: Publication) {
    override def toString: String = s"Stats(" +
      s"numberOfWords: $numberOfWords, " +
      s"numberOfPublications: $numberOfPublications, "+
      s"mostUsedWord: $mostUsedWord, "+
      s"lessUsedWord: $lessUsedWord, "+
      s"mostPopularPublication: $mostPopularPublication)"
  }



}
