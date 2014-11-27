package ru.nkdhny.runtag.filestorage

import java.net.URL
import java.nio.file.Path
import scala.concurrent.promise
import ru.nkdhny.runtag.filestorage.service.{FileOperations, FilePublisher}

import scala.concurrent.Future

/**
 * Created by alexey on 25.11.14.
 */
trait NginxFilePublisher extends FilePublisher {

  fileOperations: FileOperations =>

  val served: Map[Path, URL]
  lazy val longestPath = served.keys.map(fileOperations.tree(_).length).max

  protected def relative(url: URL, path: Path): URL

  override def publish(path: Path): Future[URL] = {
    val result = promise[URL]()

    val tree = fileOperations.tree(path)
    val mostDeep = math.min(tree.length, longestPath)
    val prefixes = Iterator.range(0, mostDeep).map(tree).toList.reverse

    prefixes.find(served.contains) match {
      case Some(servedRootPath) => {
        fileOperations.resolve(servedRootPath, path) match {
          case Some(documentPath) => result success relative(served(servedRootPath), documentPath)
          case _                  => result failure new IllegalStateException(s"${path.toString} should be child of ${servedRootPath.toString}")
        }
      }
      case _ => result failure new IllegalArgumentException(s"${path.toString} is not served")
    }

    result.future

  }
}
