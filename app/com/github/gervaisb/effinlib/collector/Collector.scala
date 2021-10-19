package com.github.gervaisb.effinlib.collector

import com.github.gervaisb.effinlib.Publication

import scala.collection.immutable

trait Collector {

  def next():immutable.Seq[Publication]

}
