package ru.nkdhny.runtag.filestorage

import java.nio.file.{Paths, Path}

import com.typesafe.config.Config
import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.service.FileOperations
import scalikejdbc._

/**
 * Created by alexey on 23.11.14.
 */

package object domain {

  type Tagged[U] = { type Tag = U }
  type @@[T, U] = T with Tagged[U]

  class Tagger[U] {
    def apply[T](t : T) : T @@ U = t.asInstanceOf[T @@ U]
  }
  def tag[U] = new Tagger[U]

  trait id[T]
  type Id[T] = String @@ id[T]

  def Id[T](t: String): Id[T] =  tag[id[T]](t)

  object Id {
    def unapply[T](id: Id[T]): Option[String] = Option(id.asInstanceOf[String])
  }

  case class ImageDescriptor(
    id: Id[ImageDescriptor],
    thumbnail: PublicPath,
    preview: PublicPath,
    safeHighResolution: Option[PublicPath]
  )

  case class UnsafeHighResolution(
    id: Id[UnsafeHighResolution],
    orig: PrivatePath
  )

  case class PublicPath(value: Path)
  case class PrivatePath(value: Path)

  object PathImplicits {
    implicit def publicPathToPath(p: PublicPath): Path = p.value
    implicit def privatePathToPath(p: PrivatePath): Path = p.value
  }
}
