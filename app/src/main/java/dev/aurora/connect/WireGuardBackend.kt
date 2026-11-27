package dev.aurora.connect

import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

// Call on a worker thread, only after explicit VPN consent.
class WireGuardBackend private constructor(private val context: Context) {
 companion object {
  private var instance: WireGuardBackend? = null

  @Synchronized fun getInstance(context: Context): WireGuardBackend =
   instance ?: WireGuardBackend(context.applicationContext).also { instance = it }
 }

 private val backend = GoBackend(context.applicationContext)
 private var tunnelDns: List<String> = emptyList()
 private val tunnel = object : Tunnel {
  override fun getName() = "aurora"
  override fun onStateChange(newState: Tunnel.State) = Unit
 }
 @Synchronized fun connect(config: Config) {
  check(VpnService.prepare(context) == null) { "VPN consent required" }
  backend.setState(tunnel, Tunnel.State.UP, config)
  tunnelDns = config.`interface`.dnsServers.mapNotNull { it.hostAddress }
 }
 @Synchronized fun disconnect() {
  backend.setState(tunnel, Tunnel.State.DOWN, null)
  tunnelDns = emptyList()
 }
 @Synchronized fun snapshot(): WireGuardSnapshot {
  val up = backend.getState(tunnel) == Tunnel.State.UP
  if (!up) {
   tunnelDns = emptyList()
   return WireGuardSnapshot(false, 0, 0, 0)
  }
  val stats = backend.getStatistics(tunnel)
  val latest = stats.peers().mapNotNull { stats.peer(it)?.latestHandshakeEpochMillis() }.maxOrNull() ?: 0L
  return WireGuardSnapshot(true, latest, stats.totalRx(), stats.totalTx(), tunnelDns)
 }
}
data class WireGuardSnapshot(
 val up: Boolean,
 val handshakeEpochMs: Long,
 val rx: Long,
 val tx: Long,
 val dnsServers: List<String> = emptyList(),
)
fun parseWireGuardConfig(text: String): Config {
 require(text.length in 1..65536)
 return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)).use { Config.parse(it) }
}
