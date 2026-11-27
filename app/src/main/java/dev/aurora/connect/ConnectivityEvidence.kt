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
