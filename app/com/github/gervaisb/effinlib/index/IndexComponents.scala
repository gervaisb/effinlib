package com.github.gervaisb.effinlib.index

import com.redis.RedisClientPool

trait IndexComponents {

  def index(redis:RedisClientPool) = new Index(redis)

}
