package ru.nkdhny.runtag.filestorage.service

import java.net.URI
import java.nio.file.{Paths, Path}

import akka.actor.ActorPath
import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.HttpFilePublisher
import ru.nkdhny.runtag.filestorage.config.ConfigSupport

import scala.concurrent.Await
import scala.concurrent.duration._
import ru.nkdhny.runtag.filestorage.domain._
/**
 * Created by alexey on 02.12.14.
 */

import FileOperations._

class HttpFilePublisherTest extends Specification{


  "HttpFilePublisher" should {

    val puplisher = new HttpFilePublisher with NoopConfig {
      override def served: Map[Path, URI] = Map(
        Paths.get("/first") -> new URI("http://served/first/_"),
        Paths.get("/first/nested") -> new URI("http://served/third/_"),
        Paths.get("/second") -> new URI("http://served/second/_")
      )


    }

    "find a served file" in {
      val served = puplisher(PublicPath(Paths.get("/first/file")))
      served must beSome
      served.get must beEqualTo(new URI("http://served/first/file"))
    }

    "find a nested served file" in {
      val served = puplisher(PublicPath(Paths.get("/first/nested/file")))
      served must beSome
      served.get must beEqualTo(new URI("http://served/third/file"))
    }

    "find a deep nested served file" in {
      val served = puplisher(PublicPath(Paths.get("/first/deep/nested/file")))
      served must beSome
      served.get must beEqualTo(new URI("http://served/first/deep/nested/file"))
    }

    "return none for files not been served" in {
      val served = puplisher(PublicPath(Paths.get("/private/deep/nested/file")))
      served must beNone
    }

  }
}
