package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Paths, Path}

import ru.nkdhny.runtag.filestorage.config.{InheritConfig, ConfigSupport}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alexey on 23.11.14.
 */
trait FilePool {

  self: ConfigSupport =>

  def publicAccess(implicit generator: UniqueGenerator, fileOperations: FileOperations): Path = {
    fileOperations.resolve(self.publicRoot, Paths.get(generator.name()))
  }
  def restrictedAccess(implicit generator: UniqueGenerator, fileOperations: FileOperations): Path = {
    fileOperations.resolve(self.privateRoot, Paths.get(generator.name()))
  }
  def persist(path: Path*)(implicit fileOperations: FileOperations) = {
    //noop
  }

  def rollback(path: Path*)(implicit fileOperations: FileOperations) = {
    for {
      p <- path
    } {
      fileOperations.remove(p)
    }
  }


}

object FilePool {

  def apply(config: ConfigSupport): FilePool = {
    new FilePool with InheritConfig {
      override val config: ConfigSupport = config
    }
  }

  object withPubicFile {

    def apply[T](op: Path => Future[T])(implicit pool: FilePool, operations: FileOperations, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp = pool.publicAccess(generator, operations)
      val ret = op(tmp)

      ret onSuccess { case _ => pool.persist(tmp)(operations)}
      ret onFailure { case _ => pool.rollback(tmp)(operations)}

      ret
    }
  }


  object withRestrictedFile {
    def apply[T](op: Path => Future[T])(implicit pool: FilePool, operations: FileOperations, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp = pool.restrictedAccess(generator, operations)
      val ret = op(tmp)

      ret onSuccess { case _ => pool.persist(tmp)(operations)}
      ret onFailure { case _ => pool.rollback(tmp)(operations)}

      ret
    }
  }

  object withPublicFiles {

    def apply[T](op: (Path, Path) => Future[T])(implicit pool: FilePool, operations: FileOperations, generator: UniqueGenerator, context: ExecutionContext) = {
      val tmp1 = pool.publicAccess(generator, operations)
      val tmp2 = pool.publicAccess(generator, operations)
      val ret = op(tmp1, tmp2)

      ret onSuccess { case _ => pool.persist(tmp1, tmp2)(operations)}
      ret onFailure { case _ => pool.rollback(tmp1, tmp2)(operations)}

      ret
    }
  }

  object withTemporaryFile {
    def apply[T](op: Path => Future[T])(implicit operations: FileOperations, context: ExecutionContext) = {
      val tmp = operations.temp

      op.apply(tmp)
    }
  }
}
