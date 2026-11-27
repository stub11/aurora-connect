package dev.aurora.connect
import org.junit.Assert.*
import org.junit.Test
class ConnectionEvidenceTest {
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
