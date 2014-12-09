package ru.nkdhny.runtag.filestorage.frontend

import java.nio.file.Path

import akka.actor.Actor
import ru.nkdhny.runtag.filestorage.cipher.Cipher
import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.db.ImageDao
import ru.nkdhny.runtag.filestorage.domain.ImageDescriptor
import ru.nkdhny.runtag.filestorage.frontend.messages.{PublishImage, LookForImage, StoreImage}
import ru.nkdhny.runtag.filestorage.service._
import akka.event.Logging
import spray.routing.HttpService

import scala.concurrent.ExecutionContext

/**
 * Created by alexey on 04.12.14.
 */

trait ImageOperationsActor extends Actor {

  import FilePool._

  val log = Logging(context.system, this)

  implicit val backend: SafeImageOperations with UnsafeImageOperations[_]
  implicit val fileOperations: FileOperations
  implicit val dao: ImageDao
  implicit val generator: UniqueGenerator
  implicit val ec: ExecutionContext
  implicit val pool: FilePool

  override def receive: Receive = {
    case StoreImage(bytes) =>
      log debug s"New image of size ${bytes.size} received"

      val task = backend.safe(bytes)

      task onSuccess {
        case image =>
          log debug s"Successfully stored image $image"
          sender ! image

      }

      task onFailure {
        case t: Throwable =>

          val tmp = withTemporaryFile(tmp => fileOperations.write(tmp, bytes))

          tmp onSuccess {
            case p: Path => log error(t, s"Was unable to store an image, you could access original data at $p")
          }

          tmp onFailure {
            case tt: Throwable =>
              log error(t, s"Was unable to store an image")
              log error(tt, s"Was unable to save original bytes")
          }

      }

    case LookForImage(id) =>
      val task = backend.descriptorFor(id)

      task onSuccess {
        case imageMaybe: Option[ImageDescriptor] => sender ! imageMaybe
      }

      task onFailure {
        case t => log error(t, s"Unable to find image $id")
      }

    case PublishImage(id, key) =>
      val task = backend.publish(id, key)

      task onSuccess {
        case imageMaybe: Option[ImageDescriptor] => sender ! imageMaybe
      }

      task onFailure {
        case t => log error(t, "Was unable to publish image")
      }
  }
}