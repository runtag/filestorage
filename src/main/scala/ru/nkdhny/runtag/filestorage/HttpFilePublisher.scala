package ru.nkdhny.runtag.filestorage

import java.net.{URI, URL}
import java.nio.file.Path
import ru.nkdhny.runtag.filestorage.config.{InheritConfig, ConfigSupport}
import ru.nkdhny.runtag.filestorage.domain.PublicPath

import scala.concurrent.promise
import ru.nkdhny.runtag.filestorage.service.{FileOperations, FilePublisher}

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */

object HttpFilePublisher {
  def apply(_config: ConfigSupport) ={
    new HttpFilePublisher with InheritConfig {
      override val config: ConfigSupport = _config
    }
  }
}

trait HttpFilePublisher extends FilePublisher {

  self: ConfigSupport =>

  def longestPath(implicit fileOperations: FileOperations) = {
    self.served.map(p => fileOperations.tree(p._1).size).max
  }

  protected def relative(url: URI, path: Path): URI = {
    url.resolve(URI.create(path.toString))
  }

  override def apply(publicPath: PublicPath)(implicit fileOperations: FileOperations): Option[URI] = {
    val path = publicPath.value
    val tree = fileOperations.tree(path).reverse
    val mostDeep = math.min(tree.length, longestPath)
    val prefixes = Iterator.range(0, mostDeep).map(tree).toList.reverse

    prefixes.find(served.contains) match {
      case Some(servedRootPath) => {
        fileOperations.relativize(servedRootPath, path) match {
          case Some(documentPath) =>
            Option(relative(served(servedRootPath), documentPath))
          case _ => None
            //new IllegalArgumentException(s"${path.toString} is not served under ${servedRootPath.toString}")

        }

      }
      case _ => None
    }

  }
}
