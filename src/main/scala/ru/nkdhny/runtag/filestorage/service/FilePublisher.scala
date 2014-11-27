package ru.nkdhny.runtag.filestorage.service

import java.net.URL
import java.nio.file.Path

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */
trait FilePublisher {
  def publish(path: Path): Future[URL]
}
