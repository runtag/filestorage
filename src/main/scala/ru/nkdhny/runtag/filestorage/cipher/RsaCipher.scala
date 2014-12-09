package ru.nkdhny.runtag.filestorage.cipher

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import javax.crypto.{Cipher =>JCipher}

import ru.nkdhny.runtag.filestorage.cipher.Cipher.KeyGenerator


/**
 * Created by alexey on 25.11.14.
 */
object Rsa {


  case class RsaPublicKey(key: PublicKey)

  implicit val rsaKeyGenerator = new KeyGenerator[RsaPublicKey] {
    override def apply(bytes: Array[Byte]): RsaPublicKey =
      RsaPublicKey(
        KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes))
      )
  }

  implicit object RsaCipher extends Cipher[RsaPublicKey] {
    val cipher = JCipher.getInstance("RSA")

    override def encrypt(input: Array[Byte], key: RsaPublicKey): Array[Byte] = {
      cipher.init(JCipher.ENCRYPT_MODE, key.key)
      cipher.doFinal(input)
    }
  }

}
