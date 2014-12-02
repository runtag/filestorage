package ru.nkdhny.runtag.filestorage.service

import ru.nkdhny.runtag.client.imageoperations.Sizer
import ru.nkdhny.runtag.filestorage.config.ConfigSupport

import scala.collection.JavaConversions
import scala.concurrent.Future
import scala.concurrent.promise
import scala.util.{Failure, Success, Try}

/**
 * Created by alexey on 23.11.14.
 */
trait LowLevelImageOperations {


  config: ConfigSupport =>

  protected def makeSize(original: Array[Byte], factor: Double) = {
    val p = promise[Array[Byte]]()

    Try(
      Sizer.produce(original, JavaConversions.seqAsJavaList(factor::Nil), config.waterMarkText, 0.2)
    ) match {
      case Success(sizes: java.util.List[Array[Byte]]) =>
        JavaConversions.iterableAsScalaIterable(sizes).headOption match {
          case Some(bytes: Array[Byte]) =>
            p success bytes
          case _ =>
            p failure new IllegalStateException("Was unable to resize on low level")
        }
      case Failure(t: Throwable) =>
        p failure t
    }

    p.future
  }

  def thumbnail(original: Array[Byte]): Future[Array[Byte]] = makeSize(original, 0.1)

  def preview(original: Array[Byte]): Future[Array[Byte]] = makeSize(original, 0.3)
}
