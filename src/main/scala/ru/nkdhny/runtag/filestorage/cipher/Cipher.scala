package ru.nkdhny.runtag.filestorage.cipher

/**
 * Created by alexey on 25.11.14.
 */
trait Cipher[T] {
  def encrypt(input: Array[Byte], key: T): Array[Byte]
}