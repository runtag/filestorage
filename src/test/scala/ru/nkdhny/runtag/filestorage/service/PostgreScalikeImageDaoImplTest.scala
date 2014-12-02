package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Paths, Path}

import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import ru.nkdhny.runtag.filestorage.db.dao.PostgreScalikeAsyncImageDao
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
      override implicit val fileOperations: FileOperations = new NioFileOperations {}
      override implicit val executionContext: ExecutionContext = ExecutionContext.global

      override def publicRoot: Path = Paths.get("/public")

      override def privateRoot: Path = Paths.get("/private")

      override def waterMarkText: String = "some text"
    }

    "save a pair of public and private image" in {
      Await.result(
        dao.safe(
          ImageDescriptor(Id("public"), Paths.get("/public/thumbnail"), Paths.get("/public/preview"), None),
          UnsafeHighResolution(Id("private"), Paths.get("/private/original"))
        ),
        100.0 seconds
      )

      true must beTrue
    }
  }

}
