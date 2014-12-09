package ru.nkdhny.runtag.filestorage.service

import java.nio.file.{Files, Paths}

import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._
/**
 * Created by alexey on 27.11.14.
 */
class NioFileOperationsTest extends Specification {

  "Nio file operations" should {

    val fileOperations = FileOperations.NioFileOperations

    "read a file as a byte array" in {
      val  bytes = Await.result(fileOperations.read(Paths.get("./src/test/resources/sample")), 1.0 seconds)
      bytes must beEqualTo("this is a sample file to be read in test".getBytes("UTF8"))
    }

    "write a byte array into a file and remove a file" in {
      val bytes = "this is a sample file to be read in test".getBytes("UTF8")

      val p = Await.result(fileOperations.write(Paths.get("./src/test/resources/a_sample"), bytes), 1.0 seconds)
      Files.exists(p) must beTrue
      val bytes_restored = Await.result(fileOperations.read(p), 1.0 seconds)

      bytes must beEqualTo(bytes_restored)
      fileOperations.remove(p)
      Files.exists(p) must beFalse
    }

    "write a file and truncate it if file exists" in {
      val old_bytes = "this is old content".getBytes("UTF8")
      Await.result(fileOperations.write(Paths.get("./src/test/resources/b_sample"), old_bytes), 1.0 seconds)

      val bytes = "this is a sample file to be read in test".getBytes("UTF8")

      val p = Await.result(fileOperations.write(Paths.get("./src/test/resources/b_sample"), bytes), 1.0 seconds)
      val bytes_restored = Await.result(fileOperations.read(p), 1.0 seconds)
      fileOperations.remove(p)

      bytes must beEqualTo(bytes_restored)
      Files.exists(p) must beFalse
    }

    "resolve a file in a directory" in {
      val root = Paths.get("/root")

      val p = fileOperations.relativize(root, Paths.get("/root/src/test/resources/sample"))

      p must beSome
      p.get must beEqualTo(Paths.get("src/test/resources/sample"))
    }

    "build a tree" in {
      val tree = fileOperations.tree(Paths.get("/one/two/tree/end"))

      tree must have size(5)
      tree must haveTheSameElementsAs(
        Paths.get("/")::
        Paths.get("/one")::
        Paths.get("/one/two")::
        Paths.get("/one/two/tree")::
        Paths.get("/one/two/tree/end")::Nil
      )
    }
  }

}
