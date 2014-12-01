package ru.nkdhny.runtag.filestorage.service

import java.nio.ByteBuffer
import java.nio.channels.{WritableByteChannel, ReadableByteChannel}
import java.nio.file._

import ru.nkdhny.runtag.filestorage.service.FileOperations

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
  def resolve(root: Path, relative:Path): Path
  def relativize(dir: Path, file: Path): Option[Path]
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

    Try(Files.write(where, what)) match {

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

  override def relativize(root: Path, absolute: Path): Option[Path] = {
    Try(root.relativize(absolute)).toOption
  }

  override def resolve(dir: Path, file: Path): Path = {
    dir.resolve(file)
  }
}
