package com.github.gervaisb.effinlib.collector

import com.typesafe.config.Config

trait CollectorComponents {

  def collector(config:Config):Collector = new TumblrCollector(config)

}
