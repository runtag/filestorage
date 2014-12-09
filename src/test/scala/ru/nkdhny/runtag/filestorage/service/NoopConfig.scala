package ru.nkdhny.runtag.filestorage.service

import java.net.URI
import java.nio.file.Path

import akka.actor.ActorPath
import ru.nkdhny.runtag.filestorage.config.ConfigSupport

import scala.concurrent.duration.FiniteDuration

/**
 * Created by alexey on 09.12.14.
 */
trait NoopConfig extends ConfigSupport {
  override def backendTimeout: FiniteDuration = null
  override def imageFrontendPath: ActorPath = null
  override def imageBackendPath: ActorPath = null
  override def waterMarkText: String = null
  override def privateRoot: Path = null
  override def publicRoot: Path = null
  override def servicePort: Int = 0
  override def served: Map[Path, URI] = null
}