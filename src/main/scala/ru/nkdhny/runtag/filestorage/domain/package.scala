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
    thumbnail: Path,
    preview: Path,
    safeHighResolution: Option[Path]
  )

  case class UnsafeHighResolution(
    id: Id[UnsafeHighResolution],
    orig: Path
  )

  object ImageDescriptor extends SQLSyntaxSupport[ImageDescriptor] {
    override val tableName = "public_images"

    def fromRelative
      (id: Id[ImageDescriptor], thumbnail: Path, preview: Path, safeHighResolution: Option[Path])
      (implicit config: ConfigSupport, files: FileOperations) : ImageDescriptor = {
      val root = config.root
      ImageDescriptor(
        id,
        files.resolve(root, thumbnail),
        files.resolve(root, preview),
        safeHighResolution.map(files.resolve(root, _)))
    }


    def apply(c: ResultName[ImageDescriptor])(rs: WrappedResultSet) = {
      fromRelative(
        Id(rs.get[String](c.id)),
        Paths.get(rs.get[String](c.thumbnail)),
        Paths.get(rs.get[String](c.preview)),
        Option(rs.get[String](c.fullSize)).map(Paths.get(_)))
    }

  }

}
