package dev.aurora.connect
import java.security.MessageDigest
fun configMetadata(text: String): String {
 val config = parseWireGuardConfig(text)
 val fingerprint = MessageDigest.getInstance("SHA-256").digest(config.`interface`.keyPair.publicKey.bytes).take(12).joinToString(":") { "%02x".format(it.toInt() and 255) }
 val endpoints = config.peers.mapNotNull { it.endpoint.orElse(null)?.toString() }.joinToString(", ")
 return "Отпечаток: $fingerprint\nEndpoint: " + endpoints.ifEmpty { "—" }
}
