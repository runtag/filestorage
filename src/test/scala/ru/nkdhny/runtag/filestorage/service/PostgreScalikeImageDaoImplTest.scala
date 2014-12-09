package ru.nkdhny.runtag.filestorage.service

import java.net.URI
import java.nio.file.{Paths, Path}

import akka.actor.ActorPath
import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.db.PostgreScalikeAsyncImageDao
import ru.nkdhny.runtag.filestorage.domain._
import scalikejdbc.async.AsyncConnectionPool

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
/**
 * Created by alexey on 02.12.14.
 */
class PostgreScalikeImageDaoImplTest extends Specification {

  "Postgre image dao" should {

    AsyncConnectionPool.singleton(
      "jdbc:postgresql://localhost:5432/runtag_filestorage", "postgres", "gai6Saes"
    )

    val dao = new PostgreScalikeAsyncImageDao with ConfigSupport {
      override implicit val fileOperations: FileOperations = FileOperations.NioFileOperations
      override implicit val executionContext: ExecutionContext = ExecutionContext.global

      override def publicRoot: Path = Paths.get("/public")

      override def privateRoot: Path = Paths.get("/private")

      override def waterMarkText: String = "some text"

      override def imageBackendPath: ActorPath = ???
      override def served: Map[Path, URI] = ???
      override def imageFrontendPath: ActorPath = ???
      override def backendTimeout: FiniteDuration = ???
      override def servicePort: Int = ???
    }

    "save a pair of public and private image" in {
      Await.result(
        dao.safe(
          ImageDescriptor(Id("public"), PublicPath(Paths.get("/public/thumbnail")), PublicPath(Paths.get("/public/preview")), None),
          UnsafeHighResolution(Id("private"), PrivatePath(Paths.get("/private/original")))
        ),
        1.0 second
      )

      val pub = Await.result(
        dao.readPublic(Id("public")),
        1.0 second
      )

      val priv = Await.result(
        dao.readPrivate(Id("public")),
        1.0 second
      )

      pub.get must beEqualTo(ImageDescriptor(Id("public"), PublicPath(Paths.get("/public/thumbnail")), PublicPath(Paths.get("/public/preview")), None))
      priv.get must beEqualTo(UnsafeHighResolution(Id("private"), PrivatePath(Paths.get("/private/original"))))

      val updated = Await.result(
        dao.publish(Id("public"), Paths.get("/public/highres")),
        1.0 second
      )

      updated must beSome
      updated.get must beEqualTo(ImageDescriptor(Id("public"), PublicPath(Paths.get("/public/thumbnail")), PublicPath(Paths.get("/public/preview")), Some(PublicPath(Paths.get("/public/highres")))))
    }
  }

}
