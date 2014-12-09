package ru.nkdhny.runtag.filestorage.service.cipher

import java.security.KeyPairGenerator
import javax.crypto.Cipher

import org.specs2.mutable._
import ru.nkdhny.runtag.filestorage.cipher.Rsa._

import scala.util.Try

/**
 * Created by alexey on 02.12.14.
 */
class RsaCipherTest extends Specification {
  "RsaCipher" should {

    "encrypt a byte array" in {
      val keyPairFactory = KeyPairGenerator.getInstance("RSA")
      keyPairFactory.initialize(512)
      val keyPair = keyPairFactory.generateKeyPair()
      val original = "This is original string" getBytes("UTF8")

      val crypted = RsaCipher.encrypt(original, RsaPublicKey(keyPair.getPublic))

      crypted mustNotEqual(original)

      val javaCipher = Cipher.getInstance("RSA")
      javaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate)
      val decrypted = javaCipher.doFinal(crypted)

      decrypted must beEqualTo(original)

    }

  }
}
