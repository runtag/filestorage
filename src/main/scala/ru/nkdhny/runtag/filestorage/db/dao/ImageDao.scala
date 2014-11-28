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
  def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]]
  def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor]
}

import scalikejdbc._
import async._, FutureImplicits._

trait PostgreScalikeAsyncImageDao extends ImageDao {

  val i = ImageDescriptor.syntax("i")

  override def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]] = {
    AsyncDB.withPool(implicit s => {
      withSQL {
        select.from(ImageDescriptor as i).where.eq(i.id, id)
      }.map(rs => ImageDescriptor(i.resultName)(rs))
    })
  }


}
