package ru.nkdhny.runtag.filestorage.service

import java.nio.ByteBuffer
import java.nio.channels.{WritableByteChannel, ReadableByteChannel}
import java.nio.file._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success, Try}

/**
 * Created by alexey on 24.11.14.
 */
trait FileOperations {
  def write(where: Path, what: Array[Byte]): Future[Path]
  def read(what: Path): Future[Array[Byte]]
  def remove(what: Path)

  /*
  Step from root to path
   */
  def tree(to: Path): List[Path]
  def resolve(root: Path, absolute:Path): Option[Path]
  def relativize(dir: Path, file: String): Path
}

trait NioFileOperations extends FileOperations {

  def read(what: Path) = {
    val ret = promise[Array[Byte]]()
    Try(Files.readAllBytes(what)) match {

      case Success(bytes: Array[Byte]) =>
        ret success bytes

      case Failure(t: Throwable) =>
        ret failure t
    }

    ret.future
  }

  override def write(where: Path, what: Array[Byte]): Future[Path] = {
    val ret = promise[Path]()

    Try(Files.write(where, what, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) match {

      case Success(written: Path) =>
        ret success written

      case Failure(t: Throwable) =>
        ret failure t
    }

    ret.future
  }

  override def remove(what: Path): Unit = Files.delete(what)

  override def tree(to: Path): List[Path] = {
    @tailrec
    def doStepBack(path: Path, visited: List[Path] = Nil): List[Path] = {
      if(path != null) {
        doStepBack(path.getParent, path::visited)
      } else {
        visited
      }
    }

    doStepBack(to).reverse
  }

  override def resolve(root: Path, absolute: Path): Option[Path] = {
    Try(absolute.relativize(root)).toOption
  }

  override def relativize(dir: Path, file: String): Path = {
    dir.resolve(file)
  }
}
