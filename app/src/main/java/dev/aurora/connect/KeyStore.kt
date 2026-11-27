package dev.aurora.connect

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedKey(val iv: ByteArray, val ciphertext: ByteArray)
interface KeyProtector {
 fun encrypt(value: ByteArray): EncryptedKey
 fun decrypt(value: EncryptedKey): ByteArray
}
class AndroidKeyProtector : KeyProtector {
 @Synchronized private fun key(): SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  val alias = "aurora-key-wrap-v1"
  (store.getKey(alias, null) as? SecretKey)?.let { return it }
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
  }.generateKey()
 }
 override fun encrypt(value: ByteArray): EncryptedKey {
  val cipher = Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE, key())
  return EncryptedKey(cipher.iv, cipher.doFinal(value))
 }
 override fun decrypt(value: EncryptedKey): ByteArray {
  val cipher = Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, value.iv))
  return cipher.doFinal(value.ciphertext)
 }
}
