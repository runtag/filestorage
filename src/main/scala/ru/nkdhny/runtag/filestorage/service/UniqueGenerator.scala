package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Files, Path}
import java.util.UUID

import ru.nkdhny.runtag.filestorage.domain.Id

import scala.util.Try

/**
 * Created by alexey on 23.11.14.
 */
trait UniqueGenerator {
  def id[T](): Id[T]
  def name(): String
}

object UniqueGenerator {

  implicit object UUIDUniqueGenerator extends UniqueGenerator {

    protected def uniqueString: String = UUID.randomUUID().toString

    override def id[T](): Id[T] = Id(uniqueString)

    override def name(): String = uniqueString

  }

}
