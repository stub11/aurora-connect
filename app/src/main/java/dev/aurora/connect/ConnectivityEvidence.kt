package dev.aurora.connect

// A response is evidence only when the caller has bound the request to the VPN network
// and verified the expected response from an explicitly configured owned endpoint.
data class ConnectivityEvidence(val vpnBound: Boolean, val expectedResponse: Boolean, val rttMs: Long?) {
 val verified: Boolean get() = vpnBound && expectedResponse && rttMs != null && rttMs >= 0
}
enum class ConnectionEvent { START, BACKEND_UP, VERIFIED, LOST, STOP }
fun transition(phase: Phase, event: ConnectionEvent): Phase = when(event) {
 ConnectionEvent.STOP -> Phase.DISCONNECTED
 ConnectionEvent.START -> if (phase == Phase.DISCONNECTED || phase == Phase.FAILED) Phase.CONNECTING else phase
 ConnectionEvent.BACKEND_UP -> phase
 ConnectionEvent.VERIFIED -> if (phase == Phase.CONNECTING) Phase.CONNECTED else phase
 ConnectionEvent.LOST -> if (phase == Phase.CONNECTED || phase == Phase.CONNECTING) Phase.FAILED else phase
}

internal enum class HandshakeState {
 INACTIVE, WAITING_FOR_TRAFFIC, WAITING_FOR_HANDSHAKE, NO_HANDSHAKE, RECEIVED
}

private const val HANDSHAKE_WAIT_MS = 20_000L

internal fun handshakeState(up: Boolean, handshakeEpochMs: Long, sinceTransmitMs: Long?): HandshakeState =
 when {
  !up -> HandshakeState.INACTIVE
  handshakeEpochMs > 0 -> HandshakeState.RECEIVED
  sinceTransmitMs == null -> HandshakeState.WAITING_FOR_TRAFFIC
  sinceTransmitMs < HANDSHAKE_WAIT_MS -> HandshakeState.WAITING_FOR_HANDSHAKE
  else -> HandshakeState.NO_HANDSHAKE
 }
