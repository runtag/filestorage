package ru.nkdhny.runtag.filestorage.service

import java.nio.file.Path

import scala.concurrent.{Future, ExecutionContext}
import com.github.nscala_time.time.Imports._

/**
 * Created by alexey on 23.11.14.
 */
trait FilePool {

  fileOperations: FileOperations =>

  val publicFileBase: Path
  val restrictedFileBase: Path


  protected def publicAccess(implicit generator: UniqueGenerator): Path = {
    generator.path(publicFileBase)
  }
  protected def restrictedAccess(implicit generator: UniqueGenerator): Path = {
    generator.path(restrictedFileBase)
  }
  protected def persist(path: Path*) = {
    //noop
  }

  protected def rollback(path: Path*) = {
    for {
      p <- path
    } {
      fileOperations.remove(p)
    }
  }


}

object FilePool {
  def withPubicFile[T](op: Path => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
    val tmp = pool.publicAccess
    val ret = op(tmp)

    ret onSuccess {case _ => pool.persist(tmp)}
    ret onFailure {case _ => pool.rollback(tmp)}

    ret
  }


  def withRestrictedFile[T](op: Path => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
    val tmp = pool.publicAccess
    val ret = op(tmp)

    ret onSuccess {case _ => pool.persist(tmp)}
    ret onFailure {case _ => pool.rollback(tmp)}

    ret
  }

  def withPublicFiles[T](op: (Path, Path) => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
    val tmp1 = pool.publicAccess
    val tmp2 = pool.publicAccess
    val ret = op(tmp1, tmp2)

    ret onSuccess {case _ => pool.persist(tmp1, tmp2)}
    ret onFailure {case _ => pool.rollback(tmp1, tmp2)}

    ret
  }
}
