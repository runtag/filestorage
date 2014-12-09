package ru.nkdhny.runtag.filestorage.service

import ru.nkdhny.runtag.filestorage.cipher.Cipher
import ru.nkdhny.runtag.filestorage.db.ImageDao
import ru.nkdhny.runtag.filestorage.domain._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.promise

/**
 * Created by alexey on 23.11.14.
 */
trait UnsafeImageOperations[T] {

  lowLevelOps: LowLevelImageOperations  =>

  import FilePool._

  implicit val fileOperations: FileOperations
  implicit val executionContext: ExecutionContext
  implicit val keyGenerator: Cipher.KeyGenerator[T]
  implicit val cipher: Cipher[T]

  private def safeOriginal(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[UnsafeHighResolution] = {
    withRestrictedFile(file => fileOperations.write(file, input)).map(original => {
        UnsafeHighResolution(generator.id(), PrivatePath(original))
    })
  }
  private def safeSizes(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[ImageDescriptor] = {
    withPublicFiles((thumbnailPath, previewPath) =>
    {
        lowLevelOps.thumbnail(input)
          .zip(lowLevelOps.preview(input))
          .flatMap(tp => {
          fileOperations.write(thumbnailPath, tp._1) zip fileOperations.write(previewPath, tp._2)
          }).map(tp => ImageDescriptor(generator.id(), PublicPath(tp._1), PublicPath(tp._2), None))

    })
  }

  def safe(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator, dao: ImageDao): Future[ImageDescriptor] = {
      lowLevelOps.safeOriginal(input) zip lowLevelOps.safeSizes(input) flatMap (op => {
        dao.safe(op._2, op._1)
      })
  }

  private def flatten[T](maybeT: Future[Option[T]]):Future[T] = {
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

  def publish(imageId: Id[ImageDescriptor], encryptionKey: Array[Byte])
                (implicit dao: ImageDao, files: FilePool, generator: UniqueGenerator):
  Future[Option[ImageDescriptor]] = {


    withPubicFile(encryptedOriginalPath => {
        flatten(dao.readPrivate(imageId)).flatMap(p => fileOperations.read(p.orig.value))
                                         .flatMap(p=> fileOperations.write(encryptedOriginalPath, Cipher(p, encryptionKey)))
                                         .flatMap(encrypted => dao.publish(imageId, encrypted))

    })
  }
}
