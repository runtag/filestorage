package ru.nkdhny.runtag.filestorage.db.dao

import java.nio.file.Path

import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id, UnsafeHighResolution}

import scala.concurrent.Future

/**
 * Created by alexey on 23.11.14.
 */
trait ImageDao {
  def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor]
  def readPrivate(id: Id[ImageDescriptor]): Future[UnsafeHighResolution]
  def readPublic(id: Id[ImageDescriptor]): Future[ImageDescriptor]
  def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor]
}
