package dev.aurora.connect

enum class Mode { LAN, FULL, SPLIT }
enum class Phase { DISCONNECTED, CONNECTING, CONNECTED, FAILED }
data class TunnelStatus(val phase: Phase = Phase.DISCONNECTED, val handshake: Boolean = false, val trafficVerified: Boolean = false, val rttMs: Long? = null, val dns: String? = null)
interface TunnelBackend {
 fun connect(mode: Mode): TunnelStatus
 fun disconnect(): TunnelStatus
}
class UnconfiguredBackend : TunnelBackend {
 override fun connect(mode: Mode) = TunnelStatus(Phase.FAILED)
 override fun disconnect() = TunnelStatus()
}
fun backoffSeconds(attempt: Int): Long = (1L shl attempt.coerceIn(0, 6)).coerceAtMost(60)
