package ru.nkdhny.runtag.filestorage.service

import scala.concurrent.Future

/**
 * Created by alexey on 23.11.14.
 */
trait LowLevelImageOperations {

  def thumbnail(original: Array[Byte]): Future[Array[Byte]]
  def preview(original: Array[Byte]): Future[Array[Byte]]
}
