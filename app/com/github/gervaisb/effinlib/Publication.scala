package com.github.gervaisb.effinlib

import java.net.URL
import java.time.OffsetDateTime

class Publication(val url:URL, val dateTime:OffsetDateTime, val popularity:Option[Int]=None) {
  override def toString: String = s"Publication(url: $url, dateTime: $dateTime, popularity: $popularity)"
}
