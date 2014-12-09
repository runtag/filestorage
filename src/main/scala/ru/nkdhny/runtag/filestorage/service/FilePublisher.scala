package ru.nkdhny.runtag.filestorage.service

import java.net.{URI, URL}
import java.nio.file.Path

import ru.nkdhny.runtag.filestorage.domain.PublicPath

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */
trait FilePublisher {
  def apply(path: PublicPath)(implicit fileOperations: FileOperations): Option[URI]
}
