package ru.nkdhny.runtag.filestorage.frontend

import java.util.concurrent.TimeoutException

import akka.actor.{Props, ActorRefFactory, Actor, FSM}
import akka.event.Logging
import ru.nkdhny.runtag.filestorage.config.{InheritConfig, ConfigSupport}
import ru.nkdhny.runtag.filestorage.domain.{PublicPath, Id, ImageDescriptor}
import ru.nkdhny.runtag.filestorage.frontend.messages.{PublishImage, LookForImage, StoreImage}
import ru.nkdhny.runtag.filestorage.service.{FilePool, FileOperations, FilePublisher}
import spray.json._
import spray.routing.{HttpService, HttpServiceActor}

import scala.concurrent.{Await, Promise, promise}
import scala.util.{Failure, Success, Try}


/**
 * Created by alexey on 06.12.14.
 */

object ImageOperationsApi {
  case class WriteTask(payload: Array[Byte], result: Promise[ImageDescriptor])
  case class ReadTask(payload: Id[ImageDescriptor], result: Promise[Option[ImageDescriptor]])
  case class PublishTask(id: Id[ImageDescriptor], key: Array[Byte], result: Promise[Option[ImageDescriptor]])
  
  trait State

  case object Idle extends State
  case object Busy extends State

  trait Data

  case object Waiting extends Data
  case class Writing(t: Array[Byte], p: Promise[ImageDescriptor]) extends Data
  case class Reading(id: Id[ImageDescriptor], p: Promise[Option[ImageDescriptor]]) extends Data
  case class TimedOut(t: StoreImage) extends Data

  object JsonProtocol extends DefaultJsonProtocol {
    implicit def idFormat[T]: RootJsonFormat[Id[T]] = new RootJsonFormat[Id[T]] {
      override def read(json: JsValue): Id[T] = json match {
        case JsString(id) => Id[T](id)
        case _ => throw new DeserializationException("Id value expected")
      }

      override def write(obj: Id[T]): JsValue = JsString(obj)
    }
    implicit def pathJsonFormat
    (implicit publisher: FilePublisher, fileOperations: FileOperations): RootJsonFormat[PublicPath] = {

      new RootJsonFormat[PublicPath] {
        override def write(obj: PublicPath): JsValue = {
          publisher(obj) match {
            case Some(u) => JsString(u.toString)
            case None => throw new SerializationException("Attempt to serialize not served path")
          }
        }

        override def read(json: JsValue): PublicPath = {
          throw new DeserializationException("Not supported")
        }
      }
    }

    implicit def imageDescriptorFormat
      (implicit publisher: FilePublisher, fileOperations: FileOperations): RootJsonFormat[ImageDescriptor] = {

      implicit val pathFormat: JsonFormat[PublicPath] = pathJsonFormat(publisher, fileOperations)
      jsonFormat4(ImageDescriptor)
    }
  }

}

import ru.nkdhny.runtag.filestorage.frontend.ImageOperationsApi._

trait ImageOperationsApi extends Actor with FSM[State, Data] {

  dependencies: ConfigSupport with FilePublisher=>

  implicit val publisher: FilePublisher = dependencies

  val backendReactor = context.actorFor(dependencies.imageBackendPath)
  val logger = Logging(context.system, this)

  when(Idle) {
    case Event(WriteTask(payload, result), Waiting) =>
      backendReactor ! StoreImage(payload)
      goto(Busy) using Writing(payload, result)
    
    case Event(ReadTask(id, p), Waiting) =>
      backendReactor ! LookForImage(id)
      goto(Busy) using Reading(id, p)

    case Event(PublishTask(id, key, result), Waiting) =>
      backendReactor ! PublishImage(id, key)
      goto(Busy) using Reading(id, result)
  }

  when(Busy, stateTimeout = dependencies.backendTimeout) {
    case Event(image: ImageDescriptor, Writing(_, result)) =>
      result success image
      goto(Idle) using Waiting

    case Event(image: Option[ImageDescriptor], Reading(_, result)) =>
      result success image
      goto(Idle) using Waiting
      
    case Event(StateTimeout, Writing(_, result)) =>
      result failure new TimeoutException(s"Was unable to save image after ${dependencies.backendTimeout}")
      goto(Idle) using Waiting


    case Event(StateTimeout, Reading(_, result)) =>
      result failure new TimeoutException(s"Was unable to read image after ${dependencies.backendTimeout}")
      goto(Idle) using Waiting
  }
  
  whenUnhandled {
    case Event(e: Event, d: Data) =>
      log.error(s"Received $e having $d, but ignore it")
      stay()
  }
  
}

import JsonProtocol._
import spray.json._
import spray.http._
import spray.routing._
import ru.nkdhny.runtag.filestorage.httpx._
import spray.httpx.SprayJsonSupport._


trait ImageOperationsHttpService extends HttpServiceActor {

  config: ConfigSupport =>

  val frontend = context.actorFor(config.imageFrontendPath)
  val log = Logging(context system, this)
  implicit val filePublisher: FilePublisher
  implicit val fileOperations: FileOperations

  val route = pathPrefix("image") {
    pathEnd{
      post {
        entity(as[Array[Byte]]) (data => {
          val result = promise[ImageDescriptor]()
          frontend ! WriteTask(data, result)
          Try(Await.result(result.future, config.backendTimeout)) match {
            case Success(image: ImageDescriptor) =>
              complete {
                image
              }

            case Failure(t: Throwable) =>
              log.error(t, "Error during image writing")
              failWith(t)
          }

        })
      }
    } ~ path(idOf[ImageDescriptor])(id => {
      get {
        val result = promise[Option[ImageDescriptor]]()
        frontend ! ReadTask(id, result)
        Try(Await.result(result.future, config.backendTimeout)) match {
          case Success(image) =>
            complete {
              image
            }
          case Failure(t: Throwable) =>
            log.error(t, "Error during image loading")
            failWith(t)
        }
      } ~ put {
        entity(as[Array[Byte]])(key => {
          val result = promise[Option[ImageDescriptor]]()
          frontend ! PublishTask(id, key, result)
          Try(Await.result(result.future, config.backendTimeout)) match {
            case Success(image) =>
              complete {
                image
              }
            case Failure(t: Throwable) =>
              log.error(t, "Error during image publishing")
              failWith(t)
          }
        })
      }
    })
  }

  def receive = runRoute(route)
}
