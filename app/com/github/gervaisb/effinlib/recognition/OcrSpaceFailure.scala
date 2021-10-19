package com.github.gervaisb.effinlib.recognition

private[recognition] class OcrSpaceFailure(status:Int, error:String, details:Option[String]) extends Throwable(
  s"Http_$status; $error"
)
