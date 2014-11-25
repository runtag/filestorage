package ru.nkdhny.runtag.filestorage.service

import java.nio.ByteBuffer
import java.nio.channels.{WritableByteChannel, ReadableByteChannel}
import java.nio.file.{StandardOpenOption, OpenOption, Files, Path}

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
  def tree(to: Path): List[Path]
  def pathFrom(root: Path, absolute:Path): Option[Path]
}

trait NioFileOperations extends FileOperations {
  private val readBufferSize = 1024

  def read(what: Path) = {
    val ret = promise[Array[Byte]]()
    Try(Files.newByteChannel(what, StandardOpenOption.READ)) match {

      case Success(channel: ReadableByteChannel) =>
        val builder = mutable.ArrayBuilder.make[Byte]()
        val buffer = ByteBuffer.allocate(readBufferSize)
        while (channel.read(buffer) > 0) {
          buffer.rewind()
          builder++=buffer.array()
          buffer.flip()
        }
        channel.close()
        ret success builder.result()

      case Failure(t: Throwable) =>
        ret failure t
    }

    ret.future
  }

  override def write(where: Path, what: Array[Byte]): Future[Path] = {
    val ret = promise[Path]()

    Try(Files.newByteChannel(where, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) match {

      case Success(channel: WritableByteChannel) =>
        val buffer = ByteBuffer.wrap(what)
        channel.write(buffer)
        channel.close()
        ret success where

      case Failure(t: Throwable) =>
        ret failure t
    }

    ret.future
  }

  override def remove(what: Path): Unit = Files.delete(what)
}
