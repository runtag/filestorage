package ru.nkdhny.runtag.filestorage

import java.io.{InputStream, OutputStream}
import java.nio.channels.ReadableByteChannel

/**
 * Created by alexey on 23.11.14.
 */
package object cipher {
  trait Cipher[T] {
    def encrypt(input: Array[Byte], key: T): Array[Byte]
  }
}
