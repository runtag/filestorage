package ru.nkdhny.runtag.filestorage

import com.github.nscala_time.time.Imports
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import ru.nkdhny.runtag.filestorage.domain.Id
import shapeless.{HNil, ::}
import spray.http.Uri.Path
import spray.httpx.unmarshalling._
import spray.routing.PathMatcher.{Unmatched, Matched, Matching}
import spray.routing.PathMatcher1

import scala.util.{Failure, Success, Try}

/**
 * Created by alexey on 01.08.14.
 */
package object httpx {

  def idOf[T]: PathMatcher1[Id[T]] = new PathMatcher1[Id[T]] {
    override def apply(p: Path): Matching[::[Id[T], HNil]] = p match {
      case Path.Segment(segment, tail) ⇒ Matched(tail, Id[T](segment) :: HNil)
      case _                           ⇒ Unmatched
    }
  }

  def id[T]: FromStringOptionDeserializer[Id[T]] = new FromStringOptionDeserializer[Id[T]] {
    override def apply(e: Option[String]): Deserialized[Id[T]] = {
      e match {
        case Some(v: String) => Right(Id(v))
        case _ => Left(ContentExpected)
      }
    }
  }

  val dateTime: FromStringOptionDeserializer[DateTime] = new FromStringOptionDeserializer[DateTime] {
    override def apply(e: Option[String]): Deserialized[Imports.DateTime] = {
      e match {
        case Some(v: String) => {
          Try(v.toLong) match {
            case Success(l: Long) => Right(new DateTime(l))
            case Failure(t: Throwable) => Left(MalformedContent("Not a long value", t))
          }
        }
        case _ => Left(ContentExpected)
      }

    }
  }

}
