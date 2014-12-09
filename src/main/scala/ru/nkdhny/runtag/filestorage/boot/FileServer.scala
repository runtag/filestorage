package ru.nkdhny.runtag.filestorage.boot

/**
 * Created by alexey on 09.12.14.
 */
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import ru.nkdhny.runtag.filestorage.HttpFilePublisher
import ru.nkdhny.runtag.filestorage.cipher.Cipher
import ru.nkdhny.runtag.filestorage.cipher.Cipher.KeyGenerator
import ru.nkdhny.runtag.filestorage.config.{ConfigSupport, InheritConfig, TypesafeConfig}
import ru.nkdhny.runtag.filestorage.db.{ImageDao, PostgreScalikeAsyncImageDao}
import ru.nkdhny.runtag.filestorage.frontend.{ImageOperationsActor, ImageOperationsHttpService}
import ru.nkdhny.runtag.filestorage.service._
import spray.can.Http

import scala.concurrent.ExecutionContext


object FileServer extends App {

  import ru.nkdhny.runtag.filestorage.cipher.Rsa._
  import ru.nkdhny.runtag.filestorage.service.UniqueGenerator.UUIDUniqueGenerator

import scala.concurrent.ExecutionContext.Implicits.global

  object ImageOperationsActor {
    def instance(_config: ConfigSupport)
                (implicit _fileOperations: FileOperations,
                 _ec: ExecutionContext,
                 _key: KeyGenerator[RsaPublicKey],
                 _cipher: Cipher[RsaPublicKey],
                  uniqueGenerator: UniqueGenerator) = {

      val _pool = FilePool(_config)

      new ImageOperationsActor {
        val _dao = new PostgreScalikeAsyncImageDao with InheritConfig {
          override val config: ConfigSupport = _config
          override implicit val fileOperations: FileOperations = _fileOperations
          override implicit val executionContext: ExecutionContext = _ec
        }

        implicit val executionContext: ExecutionContext = _ec

        override implicit val backend = new SafeImageOperations with UnsafeImageOperations[RsaPublicKey] with LowLevelImageOperations with InheritConfig {

          override val config: ConfigSupport = _config

          override implicit val fileOperations: FileOperations = _fileOperations
          override implicit val executionContext: ExecutionContext = _ec
          override implicit val keyGenerator: KeyGenerator[RsaPublicKey] = _key
          override implicit val cipher: Cipher[RsaPublicKey] = _cipher
        }

        override implicit val dao: ImageDao = _dao
        override implicit val generator: UniqueGenerator = uniqueGenerator
        override implicit val pool: FilePool = _pool
        override implicit val ec: ExecutionContext = _ec
        override implicit val fileOperations: FileOperations = _fileOperations
      }
    }

    def apply(config: ConfigSupport) = {
      Props(instance(config))
    }
  }

  object ImageOperationsHttpService {

    def instance(rootConfig: ConfigSupport)
                (implicit operations: FileOperations) = {

      val publisher = HttpFilePublisher(rootConfig)

      new ImageOperationsHttpService with InheritConfig {

        override val config: ConfigSupport = rootConfig
        override implicit val fileOperations: FileOperations = operations
        override implicit val filePublisher: FilePublisher = publisher
      }
    }

    def apply(config: ConfigSupport): Props = {
      Props(instance(config))
    }

  }

  implicit val system = ActorSystem("on-spray-can")
  val config = TypesafeConfig()
  val backend = system.actorOf(ImageOperationsActor(config), config.imageBackendPath.name)
  val frontend = system.actorOf(ImageOperationsHttpService(config), config.imageBackendPath.name)

  IO(Http) ! Http.Bind(backend, interface = "localhost", port = config.servicePort)
}