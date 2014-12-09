package ru.nkdhny.runtag.filestorage.cipher

/**
 * Created by alexey on 25.11.14.
 */
object Cipher {
  trait KeyGenerator[T] {
    def apply(bytes: Array[Byte]): T
  }

  def apply[T: KeyGenerator](input: Array[Byte], key: Array[Byte])
                            (implicit cipher: Cipher[T]): Array[Byte] = {
    cipher.encrypt(input, implicitly[KeyGenerator[T]].apply(key))
  }
}

import Cipher._

trait Cipher[T] {
  def encrypt(input: Array[Byte], key: T): Array[Byte]
}