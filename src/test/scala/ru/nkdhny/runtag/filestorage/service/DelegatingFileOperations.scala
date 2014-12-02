package ru.nkdhny.runtag.filestorage.service

import java.nio.file.Path

import scala.concurrent.Future

/**
 * Created by alexey on 02.12.14.
 */
trait DelegatingFileOperations extends FileOperations {
  val fileOperationsDelegate: FileOperations
  override def write(where: Path, what: Array[Byte]): Future[Path] = fileOperationsDelegate.write(where, what)

  override def relativize(dir: Path, file: Path): Option[Path] = fileOperationsDelegate.relativize(dir, file)

  override def remove(what: Path): Unit = fileOperationsDelegate remove(what)

  /*
    Step from root to path
     */
  override def tree(to: Path): List[Path] = fileOperationsDelegate tree(to)

  override def read(what: Path): Future[Array[Byte]] = fileOperationsDelegate read(what)

  override def resolve(root: Path, relative: Path): Path = {
    fileOperationsDelegate resolve(root, relative)
  }
}
