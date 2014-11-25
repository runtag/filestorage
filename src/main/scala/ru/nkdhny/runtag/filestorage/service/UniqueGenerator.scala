package ru.nkdhny.runtag.filestorage.service

import java.nio.file.Path

import ru.nkdhny.runtag.filestorage.domain.Id

/**
 * Created by alexey on 23.11.14.
 */
trait UniqueGenerator {
  def id[T](): Id[T]
  def name(): String
  def path(prefix: Path): Path
}
