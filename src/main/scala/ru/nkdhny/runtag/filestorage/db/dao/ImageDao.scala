package ru.nkdhny.runtag.filestorage.db.dao

import java.nio.file.Path

import ru.nkdhny.runtag.filestorage.domain.{Id, ImageDescriptor, UnsafeHighResolution}

import scala.concurrent.Future

/**
 * Created by alexey on 23.11.14.
 */
trait ImageDao {
  def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor]
  def readPrivate(id: Id[ImageDescriptor]): Future[Option[UnsafeHighResolution]]
  def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]]
  def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor]
}

