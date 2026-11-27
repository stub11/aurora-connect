package dev.aurora.connect

// Preserve imported split routes and DNS; LAN is restricted to explicitly imported private IPv4 networks.
fun modeConfig(text: String, mode: Mode): String {
 val config = parseWireGuardConfig(text)
 if (mode == Mode.SPLIT) {
  require(config.peers.none { peer -> peer.allowedIps.any { it.toString().endsWith("/0") } })
  return text
 }
 if (mode == Mode.FULL) {
  require(config.peers.size == 1)
  require(config.`interface`.dnsServers.isNotEmpty())
  return text.lineSequence().map { line ->
   if (line.substringBefore('=').trim().equals("AllowedIPs", true)) "AllowedIPs = 0.0.0.0/0, ::/0" else line
  }.joinToString("\n")
 }
 require(config.peers.all { peer -> peer.allowedIps.isNotEmpty() && peer.allowedIps.all { privateLanRoute(it.toString()) } })
 // LAN-only mode does not override system DNS.
 return text.lineSequence().filterNot { it.substringBefore('=').trim().equals("DNS", true) }.joinToString("\n")
}
fun privateLanRoute(route: String): Boolean {
 val parts = route.split('/')
 if(parts.size != 2) return false
 val a = parts[0].split('.').map { it.toIntOrNull() ?: return false }
 val prefix = parts[1].toIntOrNull() ?: return false
 if(a.size != 4 || a.any { it !in 0..255 } || prefix !in 0..32) return false
 return (a[0]==10 && prefix>=8) || (a[0]==172 && a[1] in 16..31 && prefix>=12) || (a[0]==192 && a[1]==168 && prefix>=16)
}
