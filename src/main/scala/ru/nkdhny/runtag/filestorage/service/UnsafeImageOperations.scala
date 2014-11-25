package ru.nkdhny.runtag.filestorage.service

import ru.nkdhny.runtag.filestorage.cipher.Cipher
import ru.nkdhny.runtag.filestorage.db.dao.ImageDao
import ru.nkdhny.runtag.filestorage.domain.{Id, ImageDescriptor, UnsafeHighResolution}

import scala.concurrent.Future

/**
 * Created by alexey on 23.11.14.
 */
trait UnsafeImageOperations {

  lowLevelOps: LowLevelImageOperations with FileOperations =>

  import FilePool._

  def safeOriginal(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[UnsafeHighResolution] = {
    persistAfterOperation (originalPath => {
      for {
        original <- lowLevelOps.write(originalPath, input)
      } yield {
        UnsafeHighResolution(generator.id(), original)
      }
    })
  }
  def safeSizes(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator): Future[ImageDescriptor] = {
    persistAfterOperation((thumbnailPath, previewPath) =>
    {
      for {
        thumbnailData <- lowLevelOps.thumbnail(input)
        previewData <- lowLevelOps.preview(input)
        thumbnail <- lowLevelOps.write(thumbnailPath, thumbnailData)
        preview <- lowLevelOps.write(previewPath, previewData)
      } yield {
        ImageDescriptor(generator.id(), thumbnail, preview, None)
      }
    })
  }

  def safe(input: Array[Byte])(implicit files: FilePool, generator: UniqueGenerator, dao: ImageDao): Future[ImageDescriptor] = {
    for {
      originalVersion <- lowLevelOps.safeOriginal(input)
      publicVersion   <- lowLevelOps.safeSizes(input)
      storedPublicVersion  <- dao.safe(publicVersion, originalVersion)
    } yield {
      storedPublicVersion
    }
  }
  def publish[T](imageId: Id[ImageDescriptor], encryptionKey: T)
                (implicit cipher: Cipher[T], dao: ImageDao, files: FilePool, generator: UniqueGenerator): Future[ImageDescriptor] = {
    persistAfterOperation(encryptedOriginalPath => {
      for {
        unsafeOriginal <- dao.read(imageId)
        originalData   <- lowLevelOps.read(unsafeOriginal.orig)
        encryptedVersion <- lowLevelOps.write(encryptedOriginalPath, cipher.encrypt(originalData, encryptionKey))
        updated <- dao.publish(imageId, encryptedVersion)
      } yield {
        updated
      }
    })
  }
}
