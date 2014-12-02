package ru.nkdhny.runtag.filestorage.config

import java.nio.file.Path

/**
 * Created by alexey on 28.11.14.
 */
trait ConfigSupport {

  def publicRoot: Path
  def privateRoot: Path
  def waterMarkText: String
}
