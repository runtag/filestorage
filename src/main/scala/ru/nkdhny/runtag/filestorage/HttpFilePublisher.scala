package ru.nkdhny.runtag.filestorage

import java.net.{URI, URL}
import java.nio.file.Path
import scala.concurrent.promise
import ru.nkdhny.runtag.filestorage.service.{FileOperations, FilePublisher}

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */
trait HttpFilePublisher extends FilePublisher {

  fileOperations: FileOperations =>

  val served: Map[Path, URI]
  lazy val longestPath = served.keys.map(fileOperations.tree(_).length).max

  protected def relative(url: URI, path: Path): URI = {
    url.resolve(URI.create(path.toString))
  }

  override def apply(path: Path): Future[Option[URI]] = {
    val result = promise[Option[URI]]()

    val tree = fileOperations.tree(path).reverse
    val mostDeep = math.min(tree.length, longestPath)
    val prefixes = Iterator.range(0, mostDeep).map(tree).toList.reverse

    prefixes.find(served.contains) match {
      case Some(servedRootPath) => {
        fileOperations.relativize(servedRootPath, path) match {
          case Some(documentPath) =>
            result success Option(relative(served(servedRootPath), documentPath))
          case _ => result failure new IllegalArgumentException(s"${path.toString} is not served under ${servedRootPath.toString}")

        }

      }
      case _ => result success None
    }

    result.future

  }
}
