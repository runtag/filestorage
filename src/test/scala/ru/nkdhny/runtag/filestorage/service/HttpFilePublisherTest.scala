package ru.nkdhny.runtag.filestorage.service

import java.net.URI
import java.nio.file.{Paths, Path}

import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.HttpFilePublisher

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by alexey on 02.12.14.
 */
class HttpFilePublisherTest extends Specification{


  "HttpFilePublisher" should {

    val puplisher = new HttpFilePublisher with NioFileOperations {
      override val served: Map[Path, URI] = Map(
        Paths.get("/first") -> new URI("http://served/first/_"),
        Paths.get("/first/nested") -> new URI("http://served/third/_"),
        Paths.get("/second") -> new URI("http://served/second/_")
      )
    }

    "find a served file" in {
      val served = Await.result(puplisher(Paths.get("/first/file")), 1.0 seconds)
      served must beSome
      served.get must beEqualTo(new URI("http://served/first/file"))
    }

    "find a nested served file" in {
      val served = Await.result(puplisher(Paths.get("/first/nested/file")), 1.0 seconds)
      served must beSome
      served.get must beEqualTo(new URI("http://served/third/file"))
    }

    "find a deep nested file" in {
      "find a nested served file" in {
        val served = Await.result(puplisher(Paths.get("/first/deep/nested/file")), 1.0 seconds)
        served must beSome
        served.get must beEqualTo(new URI("http://served/first/deep/nested/file"))
      }

      "return none for files not been served" in {
        val served = Await.result(puplisher(Paths.get("/private/deep/nested/file")), 1.0 seconds)
        served must beNone
      }
    }

  }
}
