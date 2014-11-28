package ru.nkdhny.runtag.filestorage.db.dao

import java.nio.file.{Paths, Path}

import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id, UnsafeHighResolution}
import ru.nkdhny.runtag.filestorage.service.FileOperations

import scala.concurrent.{Future, promise, future}

/**
 * Created by alexey on 23.11.14.
 */
trait ImageDao {
  def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor]
  def readPrivate(id: Id[ImageDescriptor]): Future[Option[UnsafeHighResolution]]
  def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]]
  def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor]
}

import scalikejdbc._
import async._, FutureImplicits._

trait PostgreScalikeAsyncImageDao extends ImageDao {
  config: ConfigSupport=>

  object ImageDescriptorSyntax extends SQLSyntaxSupport[ImageDescriptor] {
    override val tableName = "public_images"
  }

  object UnsafeHighResolutionSyntax extends SQLSyntaxSupport[UnsafeHighResolution] {
    override val tableName = "private_image"
  }


  val i = ImageDescriptorSyntax.syntax("i")
  val u = UnsafeHighResolutionSyntax.syntax("u")
  implicit val fileOperations: FileOperations

  override def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]] = {
    AsyncDB.withPool(implicit s => {
      withSQL {
        select.from(ImageDescriptorSyntax as i).where.eq(i.id, id)
      }.map(rs => {

        ImageDescriptor(
          Id(rs.get[String](i.id)),
          fileOperations.resolve(config.root, Paths.get(rs.get[String](i.thumbnail))),
          fileOperations.resolve(config.root, Paths.get(rs.get[String](i.preview))),
          rs.get[Option[String]](i.highResolution).map(Paths.get(_)).map(fileOperations.resolve(config.root, _))
        )
      })
    })
  }

  def persistImageDescriptor(image: ImageDescriptor, original: UnsafeHighResolution)(implicit s: AsyncDBSession): Future[ImageDescriptor] = {
    val ret = promise[ImageDescriptor]()


    def relativize(p: Path) = future {
      fileOperations.relativize(config.root, p)
        .getOrElse(throw new IllegalArgumentException(s"File ${p} is not a child of ${config.root}"))
    }

    for{

      thumbnail <- relativize(image.thumbnail).map(_.toString)
      preview   <- relativize(image.preview).map(_.toString)
      safeHighResolution <- image.safeHighResolution.map(relativize).map(_.toString).getOrElse({
        val nothing = promise[Path]()
        nothing.success(null)
        nothing.future
      })
      rows_updated <- withSQL {
        insert.into(ImageDescriptorSyntax)
          .values(
            image.id,
            thumbnail,
            preview,
            safeHighResolution,
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

  def persistOriginal(original: UnsafeHighResolution)(implicit s: AsyncDBSession): Future[UnsafeHighResolution] = {
    val ret = promise[UnsafeHighResolution]()

    for {
      relative_path <- future {
        fileOperations.relativize(config.root, original.orig)
                      .getOrElse(throw new IllegalArgumentException(s"File ${original.orig} is not a child of ${config.root}"))
      }.map(_.toString)

      rows_updated <- withSQL {
        insert.into(UnsafeHighResolutionSyntax).values(
          original.id,
          relative_path
        )
      }.update()

    } yield {
      if(rows_updated == 1) {
        ret success original
      } else if(rows_updated > 0) {
        ret failure new IllegalStateException(s"after insert is performed $rows_updated rows were touched")
      } else {
        ret failure new IllegalStateException(s"Insert was performed but nothin updated")
      }
    }

    ret.future
  }

  override def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor] = {
    AsyncDB.localTx(implicit s =>{
        for {
          restricted <- persistOriginal(privateImage)
          descriptor <- persistImageDescriptor(publicImage, restricted)
        } yield {
          descriptor
        }
    })
  }

  override def readPrivate(id: Id[ImageDescriptor]): Future[Option[UnsafeHighResolution]] = {
    AsyncDB.withPool(implicit s=>
      withSQL {
        select.from(UnsafeHighResolutionSyntax as u)
          .join(ImageDescriptorSyntax as i)
          .on(u.id, i.original_id)
          .where.eq(i.id, id)
      }.map(rs => {
        UnsafeHighResolution(
          Id(rs.get[String](u.id)),
          fileOperations.resolve(config.root, Paths.get(rs.get[String](u.original)))
        )
      })
    )
  }

  override def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor] = {
    val ret = promise[ImageDescriptor]()

    AsyncDB.withPool(implicit s => {
      for {
        rows_updated <- withSQL {
          update(ImageDescriptorSyntax).where.eq(i.id, id)
        }.update()
      } yield {
        if(rows_updated == 1) {
          val readTask = readPublic(id)

          readTask onSuccess {
            case Some(updated: ImageDescriptor) =>  ret success updated
            case _ => ret failure new IllegalArgumentException(s"Image with id $id was updated but not recieved")
          }

          readTask onFailure {
            case t: Throwable => ret failure t
          }
        } else {
          ret failure new IllegalArgumentException(s"Unable to publish image, image with id $id not found")
        }
      }
    })

    ret.future
  }
}
