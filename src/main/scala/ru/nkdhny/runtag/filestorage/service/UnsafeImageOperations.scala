package ru.nkdhny.runtag.filestorage.service

import ru.nkdhny.runtag.filestorage.cipher.Cipher
import ru.nkdhny.runtag.filestorage.db.dao.ImageDao
import ru.nkdhny.runtag.filestorage.domain.{Id, ImageDescriptor, UnsafeHighResolution}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.promise

/**
 * Created by alexey on 23.11.14.
 */
trait UnsafeImageOperations {

  lowLevelOps: LowLevelImageOperations with FileOperations =>

  import FilePool._

  implicit val executionContext: ExecutionContext

  def safeOriginal(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[UnsafeHighResolution] = {
      lowLevelOps.write(files.restrictedAccess, input).map(original => {
        UnsafeHighResolution(generator.id(), original)
    })
  }
  def safeSizes(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[ImageDescriptor] = {
    withPublicFiles((thumbnailPath, previewPath) =>
    {
        lowLevelOps.thumbnail(input)
          .zip(lowLevelOps.preview(input))
          .flatMap(tp => {
            lowLevelOps.write(thumbnailPath, tp._1) zip lowLevelOps.write(previewPath, tp._2)
          }).map(tp => ImageDescriptor(generator.id(), tp._1, tp._2, None))

    })
  }

  def safe(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator, dao: ImageDao): Future[ImageDescriptor] = {
      lowLevelOps.safeOriginal(input) zip lowLevelOps.safeSizes(input) flatMap (op => {
        dao.safe(op._2, op._1)
      })
  }

  def flatten[T](maybeT: Future[Option[T]]):Future[T] = {
    val ret = promise[T]()

    maybeT.onSuccess {
      case Some(t) => ret success t
      case _ => ret failure new IllegalStateException("Value expected")
    }

    maybeT onFailure {
      case t: Throwable => ret failure t
    }

    ret.future
  }

  def publish[T](imageId: Id[ImageDescriptor], encryptionKey: T)
                (implicit cipher: Cipher[T], dao: ImageDao, files: FilePool, generator: UniqueGenerator): Future[ImageDescriptor] = {


    withPubicFile(encryptedOriginalPath => {
        flatten(dao.readPrivate(imageId)).flatMap(p => lowLevelOps.read(p.orig))
                                         .flatMap(p=> lowLevelOps.write(encryptedOriginalPath, cipher.encrypt(p, encryptionKey)))
                                         .flatMap(encrypted => dao.publish(imageId, encrypted))

    })
  }
}
