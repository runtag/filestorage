package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Paths, Path}

import ru.nkdhny.runtag.filestorage.config.ConfigSupport

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alexey on 23.11.14.
 */
trait FilePool {

  self: FileOperations with ConfigSupport =>

  def publicAccess(implicit generator: UniqueGenerator): Path = {
    self.resolve(self.publicRoot, Paths.get(generator.name()))
  }
  def restrictedAccess(implicit generator: UniqueGenerator): Path = {
    self.resolve(self.privateRoot, Paths.get(generator.name()))
  }
  def persist(path: Path*) = {
    //noop
  }

  def rollback(path: Path*) = {
    for {
      p <- path
    } {
      self.remove(p)
    }
  }


}

object FilePool {
  object withPubicFile {

    def apply[T](op: Path => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp = pool.publicAccess
      val ret = op(tmp)

      ret onSuccess { case _ => pool.persist(tmp)}
      ret onFailure { case _ => pool.rollback(tmp)}

      ret
    }
  }


  object withRestrictedFile {
    def apply[T](op: Path => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp = pool.restrictedAccess
      val ret = op(tmp)

      ret onSuccess { case _ => pool.persist(tmp)}
      ret onFailure { case _ => pool.rollback(tmp)}

      ret
    }
  }

  object withPublicFiles {

    def apply[T](op: (Path, Path) => Future[T])(implicit pool: FilePool, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp1 = pool.publicAccess
      val tmp2 = pool.publicAccess
      val ret = op(tmp1, tmp2)

      ret onSuccess { case _ => pool.persist(tmp1, tmp2)}
      ret onFailure { case _ => pool.rollback(tmp1, tmp2)}

      ret
    }
  }
}
