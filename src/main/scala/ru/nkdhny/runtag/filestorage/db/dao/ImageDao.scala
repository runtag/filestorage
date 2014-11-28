package ru.nkdhny.runtag.filestorage.db.dao

import java.nio.file.{Paths, Path}

import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id, UnsafeHighResolution}
import ru.nkdhny.runtag.filestorage.service.FileOperations

import scala.concurrent.{Future, promise, future}

/**
 * Created by alexey on 23.11.14.
 */
trait ImageDao {
  def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor]
  def readPrivate(id: Id[ImageDescriptor]): Future[Option[UnsafeHighResolution]
  def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]]
  def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor]
}

