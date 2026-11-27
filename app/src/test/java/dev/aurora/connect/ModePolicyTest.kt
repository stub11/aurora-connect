package dev.aurora.connect
import org.junit.Test
import org.junit.Assert.*
class ModePolicyTest {
 private val config = """
  [Interface]
  PrivateKey = AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=
  Address = 10.0.0.2/32
  DNS = 10.0.0.1

  [Peer]
  PublicKey = AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI=
  Endpoint = 192.0.2.1:51820
 """.trimIndent()

 @Test(expected = IllegalArgumentException::class)
 fun fullTunnelRequiresAnAllowedIpsEntry() {
  modeConfig(config, Mode.FULL)
 }

 @Test fun fullTunnelAddsBothDefaultRoutes() {
  val full = modeConfig("$config\nAllowedIPs = 10.0.0.0/24", Mode.FULL)
  assertTrue(full.lineSequence().any { it == "AllowedIPs = 0.0.0.0/0, ::/0" })
 }
 @Test fun lanRanges() {
  assertTrue(privateLanRoute("192.168.0.0/16"))
  assertTrue(privateLanRoute("172.16.0.0/12"))
  assertFalse(privateLanRoute("172.16.0.0/11"))
  assertFalse(privateLanRoute("0.0.0.0/0"))
  assertFalse(privateLanRoute("192.168.0.0/15"))
  assertFalse(privateLanRoute("::/0"))
 }
}
