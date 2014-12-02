package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Paths, Path}

import org.specs2.mock.Mockito
import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.config.ConfigSupport
import scala.concurrent.future

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by alexey on 02.12.14.
 */
class FilePoolTest extends Specification with Mockito {


  def filePool = new FilePool with DelegatingFileOperations with ConfigSupport {
    val operations: FileOperations = mock[FileOperations]

    operations.resolve(any[Path], any[Path]) answers((params, m) => {
      params.asInstanceOf[Array[Any]](0).asInstanceOf[Path]
        .resolve(params.asInstanceOf[Array[Any]](1).asInstanceOf[Path])
    })

    override val fileOperationsDelegate: FileOperations = operations

    override def publicRoot: Path = Paths.get("/public")

    override def privateRoot: Path = Paths.get("/private")

    override def waterMarkText: String = "some text"


  }


  import FilePool._

  "File pool" should {
    "persist  public file after successful operation" in {
      implicit val pool = filePool
      implicit val generator = mock[UniqueGenerator]

      generator.name() returns("file")


      withPubicFile( f => {
        f.toString must beEqualTo("/public/file")
        future {
          //noop
        }
      })
      there was no(pool.operations).remove(Paths.get("/public/file"))
    }
    "persist  restricted file after successful operation" in {
      implicit val pool = filePool
      implicit val generator = mock[UniqueGenerator]

      generator.name() returns("file")


      withRestrictedFile( f => {
        f.toString must beEqualTo("/private/file")
        future {
          //noop
        }
      })
      there was no(pool.operations).remove(Paths.get("/public/file"))
    }
    "remove a file with failed operation" in {
      implicit val pool = filePool
      implicit val generator = mock[UniqueGenerator]

      generator.name() returns("file")


      withPubicFile(f => {
        f.toString must beEqualTo("/public/file")
        future {
          throw new Exception()
        }
      })
      there was one(pool.operations).remove(Paths.get("/public/file"))
    }
  }
}
