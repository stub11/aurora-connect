package dev.aurora.connect
import org.junit.Assert.*
import org.junit.Test
class TunnelTest {
 private class FakeBackend : TunnelBackend {
  override fun connect(mode: Mode) = TunnelStatus(Phase.CONNECTED, handshake = true)
  override fun disconnect() = TunnelStatus()
 }
 @Test fun handshakeDoesNotProveTraffic() {
  val backend = FakeBackend()
  assertTrue(backend.connect(Mode.LAN).handshake)
  assertFalse(backend.connect(Mode.FULL).trafficVerified)
  assertEquals(Phase.DISCONNECTED, backend.disconnect().phase)
 }
 @Test fun unconfiguredFailsClosed() {
  assertEquals(Phase.FAILED, UnconfiguredBackend().connect(Mode.FULL).phase)
 }
 @Test fun boundedBackoff() {
  assertEquals(1L, backoffSeconds(-1))
  assertEquals(8L, backoffSeconds(3))
  assertEquals(60L, backoffSeconds(Int.MAX_VALUE))
 }
}
