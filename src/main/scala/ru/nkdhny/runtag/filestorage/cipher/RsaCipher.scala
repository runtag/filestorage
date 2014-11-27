package ru.nkdhny.runtag.filestorage.cipher

import java.security.PublicKey
import javax.crypto.Cipher

import ru.nkdhny.runtag.filestorage.cipher.RsaCipher.RsaPublicKey

/**
 * Created by alexey on 25.11.14.
 */
object RsaCipher {
  case class RsaPublicKey(key: PublicKey)
}

class RsaCipher extends Cipher[RsaPublicKey] {
  val cipher = Cipher.getInstance("RSA")
  override def encrypt(input: Array[Byte], key: RsaPublicKey): Array[Byte] = {
    cipher.init(Cipher.ENCRYPT_MODE, key.key)
    cipher.doFinal(input)
  }
}
