package dev.aurora.connect
import org.junit.Assert.*
import org.junit.Test
class ConnectionEvidenceTest {
 @Test fun probeRequiresHttpsWithoutCredentialsOrParameters() {
  assertEquals("192.0.2.4", parseProbeUri("https://192.0.2.4/health").host)
  assertEquals("[2001:db8::3]", parseProbeUri("https://[2001:db8::3]/health").host)
  listOf("http://192.0.2.4", "https://user:pass@192.0.2.4",
   "https://192.0.2.4/?key=value", "https://192.0.2.4/#x",
   "https://192.0.2.4:65536").forEach { assertTrue(runCatching { parseProbeUri(it) }.isFailure) }
 }
 @Test fun invalidConfigRejected() {
  assertTrue(runCatching { parseWireGuardConfig("[Interface]\nPrivateKey = invalid") }.isFailure)
 }
 @Test fun handshakeIsNotConnectivity() {
  val connecting = transition(Phase.DISCONNECTED, ConnectionEvent.START)
  assertEquals(Phase.CONNECTING, transition(connecting, ConnectionEvent.BACKEND_UP))
  assertFalse(ConnectivityEvidence(false, true, 10).verified)
  assertTrue(ConnectivityEvidence(true, true, 10).verified)
  assertEquals(Phase.CONNECTED, transition(connecting, ConnectionEvent.VERIFIED))
  assertEquals(Phase.FAILED, transition(Phase.CONNECTED, ConnectionEvent.LOST))
  assertEquals(Phase.DISCONNECTED, transition(Phase.FAILED, ConnectionEvent.STOP))
 }
}
