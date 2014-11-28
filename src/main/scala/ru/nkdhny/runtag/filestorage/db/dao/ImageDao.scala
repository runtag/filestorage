package ru.nkdhny.runtag.filestorage.db.dao

import java.nio.file.Path

import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id, UnsafeHighResolution}
import ru.nkdhny.runtag.filestorage.service.FileOperations

import scala.concurrent.{Future, promise}

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
  config: ConfigSupport=>

  val i = ImageDescriptor.syntax("i")
  implicit val fileOperations: FileOperations

  override def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]] = {
    AsyncDB.withPool(implicit s => {
      withSQL {
        select.from(ImageDescriptor as i).where.eq(i.id, id)
      }.map(rs => ImageDescriptor(i.resultName)(rs))
    })
  }

  def safeImageDescriptor(image: ImageDescriptor, original: UnsafeHighResolution)(implicit s: AsyncDBSession): Future[ImageDescriptor] = {
    val ret = promise[ImageDescriptor]()

    for{
      rows_updated <- withSQL {
        insert.into(ImageDescriptor)
          .values(
            image.id,
            fileOperations.relativize(config.root, image.thumbnail),
            fileOperations.relativize(config.root, image.preview),
            image.safeHighResolution.map(fileOperations.relativize(config.root, _)).orNull,
            original.id)
      }.update()
    } yield {
      val readTask = readPublic(image.id)

      readTask onSuccess  {
        case Some(written: ImageDescriptor) =>
          ret success written
        case _ =>
          ret failure new IllegalStateException(s"Saved $rows_updated rows but public image with id ${image.id} was not found")
      }

      readTask onFailure {
        case t: Throwable => ret failure t
      }
    }

    ret.future
  }

  def safeUnsafeImage(original: UnsafeHighResolution): Future[UnsafeHighResolution] = {
    ???
  }

  override def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor] = {
    AsyncDB.localTx(implicit s =>{
        for {
          restricted <- safeUnsafeImage(privateImage)
          pub <- safeImageDescriptor(publicImage, restricted)
        } yield {
          pub
        }
    })
  }
}
