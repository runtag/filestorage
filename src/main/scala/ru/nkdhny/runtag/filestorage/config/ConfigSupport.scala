package ru.nkdhny.runtag.filestorage.config

import java.net.URI
import java.nio.file.{Paths, Path}

import akka.actor.ActorPath
import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
 * Created by alexey on 28.11.14.
 */
trait ConfigSupport {

  def publicRoot: Path
  def privateRoot: Path
  def waterMarkText: String
  def imageBackendPath: ActorPath
  def imageFrontendPath: ActorPath
  def backendTimeout: FiniteDuration
  def served: Map[Path, URI]
  def servicePort: Int
}

object TypesafeConfig {
  def apply() = {
    new TypesafeConfig {
      override val config: Config = ConfigFactory.load()
    }
  }
}

trait TypesafeConfig extends ConfigSupport {
  val config: Config

  override def publicRoot: Path = {
    Paths.get(config.getString("files.public"))
  }

  override def privateRoot: Path = {
    Paths.get(config.getString("files.private"))
  }

  override def waterMarkText: String = {
    config.getString("images.text")
  }

  override def imageBackendPath: ActorPath = ActorPath.fromString(config.getString("backend.path"))

  override def served: Map[Path, URI] =
    iterableAsScalaIterable(config.getConfigList("files.served"))
      .map(entry => entry.getString("from") -> entry.getString("to"))
      .map(entry=> Paths.get(entry._1) -> URI.create(entry._2))
      .toMap

  override def imageFrontendPath: ActorPath = ActorPath.fromString(config.getString("frontend.path"))
  override def backendTimeout: FiniteDuration = config.getInt("backend.timeout.microseconds") microsecond

  override def servicePort: Int = config.getInt("frontend.port")
}

trait InheritConfig extends ConfigSupport {
  val config: ConfigSupport
  override def publicRoot: Path = config.publicRoot
  override def privateRoot: Path = config.privateRoot
  override def waterMarkText: String = config.waterMarkText
  override def imageBackendPath: ActorPath = config.imageBackendPath
  override def imageFrontendPath: ActorPath = config.imageFrontendPath
  override def backendTimeout: FiniteDuration = config.backendTimeout
  override def served: Map[Path, URI] = config.served
  override def servicePort: Int = config.servicePort
}