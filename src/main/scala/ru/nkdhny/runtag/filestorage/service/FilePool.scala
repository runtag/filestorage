package ru.nkdhny.runtag.filestorage.service

import java.nio.file.Path

/**
 * Created by alexey on 23.11.14.
 */
trait FilePool {

  protected def temporary(implicit generator: UniqueGenerator): Path
  protected def persist(path: Path*)


}

object FilePool {
  def persistAfterOperation[T](op: Path => T)(implicit pool: FilePool, generator: UniqueGenerator) = {
    val tmp = pool.temporary
    val ret = op(tmp)
    pool.persist(tmp)
    ret
  }

  def persistAfterOperation[T](op: (Path, Path) => T)(implicit pool: FilePool, generator: UniqueGenerator) = {
    val tmp1 = pool.temporary
    val tmp2 = pool.temporary
    val ret = op(tmp1, tmp2)
    pool.persist(tmp1, tmp2)
    ret
  }
}
