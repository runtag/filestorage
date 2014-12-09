package ru.nkdhny.runtag.filestorage.frontend

import ru.nkdhny.runtag.filestorage.domain.{ImageDescriptor, Id}


/**
 * Created by alexey on 06.12.14.
 */
package object messages {
  case class StoreImage(data: Array[Byte])
  case class LookForImage(id: Id[ImageDescriptor])
  case class PublishImage(id: Id[ImageDescriptor], key:Array[Byte])
}
