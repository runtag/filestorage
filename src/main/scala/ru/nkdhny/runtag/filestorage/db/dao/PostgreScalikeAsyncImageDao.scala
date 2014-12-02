package ru.nkdhny.runtag.filestorage.db.dao

/**
 * Created by alexey on 28.11.14.
 */

import java.nio.file.{Path, Paths}

import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.domain._
import ru.nkdhny.runtag.filestorage.service.FileOperations
import scalikejdbc._
import async._, FutureImplicits._

import scala.concurrent._

trait PostgreScalikeAsyncImageDao extends ImageDao {
  config: ConfigSupport=>

  implicit val executionContext: ExecutionContext
  implicit val fileOperations: FileOperations

  object ImageDescriptorSyntax extends SQLSyntaxSupport[ImageDescriptor] {
    override val tableName = "public_image"
    override val columns = "id"::"thumbnail"::"preview"::"safe_high_resolution"::"original_id"::Nil
  }

  object UnsafeHighResolutionSyntax extends SQLSyntaxSupport[UnsafeHighResolution] {
    override val tableName = "private_image"
    override val columns = "id"::"orig"::Nil
  }


  val i = ImageDescriptorSyntax.syntax("i")
  val u = UnsafeHighResolutionSyntax.syntax("u")

  override def readPublic(id: Id[ImageDescriptor]): Future[Option[ImageDescriptor]] = {
    def resolveOptionalPath(rs: WrappedResultSet): Option[Path] = {
      rs.get[Option[String]](i.safeHighResolution)
        .map(Paths.get(_))
        .map(fileOperations.resolve(config.publicRoot, _))
    }

    AsyncDB.withPool(implicit s => {
      withSQL {
        select.from(ImageDescriptorSyntax as i).where.eq(i.id, id)
      }.map(rs => {

        ImageDescriptor(
          Id(rs.get[String](i.id)),
          fileOperations.resolve(config.publicRoot, Paths.get(rs.get[String](i.thumbnail))),
          fileOperations.resolve(config.publicRoot, Paths.get(rs.get[String](i.preview))),
          resolveOptionalPath(rs)
        )
      })
    })
  }


  override def safe(publicImage: ImageDescriptor, privateImage: UnsafeHighResolution): Future[ImageDescriptor] = {

    def persistImageDescriptor(image: ImageDescriptor, original: UnsafeHighResolution)
                              (implicit s: AsyncDBSession): Future[ImageDescriptor] = {
      val ret = promise[ImageDescriptor]()

      val thumbnail = fileOperations.relativize(config.publicRoot, publicImage.thumbnail)
        .map(_.toString)


      val preview = fileOperations.relativize(config.publicRoot, publicImage.preview)
        .map(_.toString)

      val highRes = publicImage.safeHighResolution
        .flatMap(fileOperations.relativize(config.publicRoot, _))
        .map(_.toString)
        .orNull

      val paths =promise[(String, String, String)]()

      thumbnail match {
        case Some(t) =>
          preview match {
            case Some(p) => paths success (t, p, highRes)
            case _ => paths failure new IllegalStateException(s"Unable to resolve ${publicImage.preview} against ${config.publicRoot}")
          }
        case _ => paths failure new IllegalStateException(s"Unable to resolve ${publicImage.thumbnail} against ${config.publicRoot}")
      }


      val insert_result = paths.future
        .flatMap(tps=> withSQL {
          insert.into(ImageDescriptorSyntax)
              .values(
                image.id,
                tps._1,
                tps._2,
                tps._3,
                original.id)
          }.update())

      insert_result onSuccess {
        case rows_updated: Int if rows_updated == 1 =>
          ret success image
        case rows_updated: Int if rows_updated == 0 =>
          ret failure new IllegalStateException(s"Was unable to save public image with id ${image.id}, nothing has been written")
        case _ =>
          ret failure new IllegalStateException(s"Was unable to save public image with id ${image.id}")

      }

      insert_result onFailure {
        case t: Throwable => ret failure t
      }


      ret.future
    }

    def persistOriginal(original: UnsafeHighResolution)(implicit s: AsyncDBSession): Future[UnsafeHighResolution] = {
      val ret = promise[UnsafeHighResolution]()
      val relative_path = promise[String]()

      fileOperations.relativize(config.privateRoot, original.orig) match {
        case Some(p)=> relative_path success p.toString
        case _ => relative_path failure new IllegalStateException(s"Unable to resolve ${original.orig} against ${config.privateRoot}")
      }


      val insert_result = relative_path.future
        .flatMap(p => withSQL {
          insert.into(UnsafeHighResolutionSyntax).values(
            original.id,
            p
          )
          }.update())

      insert_result onSuccess {
        case rows_updated: Int => {
          if (rows_updated == 1) {
            ret success original
          } else if (rows_updated > 0) {
            ret failure new IllegalStateException(s"after insert is performed $rows_updated rows were touched")
          } else {
            ret failure new IllegalStateException(s"Insert was performed but nothing updated")
          }
        }
      }

      insert_result onFailure {
        case t: Throwable => ret failure t
      }

      ret.future
    }

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
          .on(u.id, i.column("original_id"))
          .where.eq(i.id, id)
      }.map(rs => {
        UnsafeHighResolution(
          Id(rs.get[String](u.id)),
          fileOperations.resolve(config.privateRoot, Paths.get(rs.get[String](u.orig)))
        )
      })
    )
  }

  override def publish(id: Id[ImageDescriptor], publicVersion: Path): Future[ImageDescriptor] = {
    val ret = promise[ImageDescriptor]()

    val update_result = AsyncDB.withPool(implicit s => {
     withSQL {
        update(ImageDescriptorSyntax).set(i.safeHighResolution -> publicVersion.toString).where.eq(i.id, id)
      }.update()
    })

    update_result onSuccess {
      case rows_updated: Int if rows_updated == 1 =>
        val readTask = readPublic(id)

        readTask onSuccess {
          case Some(updated: ImageDescriptor) => ret success updated
          case _ => ret failure new IllegalArgumentException(s"Image with id $id was updated but not found")
        }

        readTask onFailure {
          case t: Throwable => ret failure t
        }

      case rows_updated: Int if rows_updated == 0 => ret failure new IllegalArgumentException(s"Nothing updated with id: $id")
    }

    update_result onFailure {
      case t: Throwable => ret failure t
    }


    ret.future
  }
}
