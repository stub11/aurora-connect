package dev.aurora.connect
import android.content.Context
import android.util.Base64

// Encrypt entire configuration, including private key, before persistence.
class EncryptedConfigStore(context: Context, private val protector: KeyProtector = AndroidKeyProtector()) {
 private val prefs = context.getSharedPreferences("encrypted_tunnel", Context.MODE_PRIVATE)
 fun save(text: String) {
  parseWireGuardConfig(text)
  val bytes = text.toByteArray(Charsets.UTF_8)
  try {
   val encrypted = protector.encrypt(bytes)
   check(prefs.edit().putString("iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
    .putString("ciphertext", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP)).commit())
  } finally { bytes.fill(0) }
 }
 fun load(): String? {
  val iv = prefs.getString("iv", null) ?: return null
  val ciphertext = prefs.getString("ciphertext", null) ?: return null
  val bytes = protector.decrypt(EncryptedKey(Base64.decode(iv, Base64.NO_WRAP), Base64.decode(ciphertext, Base64.NO_WRAP)))
  return try { bytes.toString(Charsets.UTF_8) } finally { bytes.fill(0) }
 }
 fun clear() { check(prefs.edit().clear().commit()) }
}
