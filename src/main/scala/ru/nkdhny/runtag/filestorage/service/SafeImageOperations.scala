package ru.nkdhny.runtag.filestorage.service

import ru.nkdhny.runtag.filestorage.db.ImageDao
import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id}

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */
trait SafeImageOperations {

  def descriptorFor(id: Id[ImageDescriptor])(implicit dao: ImageDao): Future[Option[ImageDescriptor]] = {
    dao.readPublic(id)
  }
}
