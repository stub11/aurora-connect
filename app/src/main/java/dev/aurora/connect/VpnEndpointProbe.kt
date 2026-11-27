package dev.aurora.connect

import android.content.Context
import android.net.ConnectivityManager
import android.net.InetAddresses
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.net.Proxy
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal fun parseProbeUri(text: String): URI {
    val value = text.trim()
    require(value.length in 1..2048)
    val uri = URI(value)
    require(uri.scheme.equals("https", ignoreCase = true) &&
        uri.host != null && uri.rawUserInfo == null &&
        uri.rawQuery == null && uri.rawFragment == null &&
        (uri.port == -1 || uri.port in 1..65535))
    return uri
}

internal class ProbeRejected(val notice: String) : Exception(notice)

internal fun checkVpnEndpoint(context: Context, text: String): ConnectivityEvidence {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        throw ProbeRejected("Проверка доступна на Android 11 и новее")
    }
    val uri = try {
        parseProbeUri(text)
    } catch (_: Exception) {
        throw ProbeRejected("Нужен HTTPS-адрес без логина, параметров и фрагмента")
    }
    val target = try {
        InetAddresses.parseNumericAddress(uri.host.removeSurrounding("[", "]"))
    } catch (_: IllegalArgumentException) {
        throw ProbeRejected("Укажи IP-адрес тестового узла в ссылке HTTPS")
    }
    val manager = context.getSystemService(ConnectivityManager::class.java)
        ?: throw ProbeRejected("Служба проверки сети Android недоступна")
    val vpn = manager.activeNetwork
        ?: throw ProbeRejected("Активная VPN-сеть Aurora Connect недоступна")
    fun currentVpn(network: Network): Boolean {
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return manager.activeNetwork == network &&
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            capabilities.ownerUid == Process.myUid()
    }
    if (!currentVpn(vpn)) throw ProbeRejected("Активная VPN-сеть Aurora Connect недоступна")
    val routes = manager.getLinkProperties(vpn)?.routes.orEmpty()
    if (routes.none { it.destination.contains(target) }) {
        throw ProbeRejected("До этого IP нет маршрута в VPN")
    }

    val connection = vpn.openConnection(uri.toURL(), Proxy.NO_PROXY) as HttpsURLConnection
    return try {
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val start = SystemClock.elapsedRealtime()
        val status = connection.responseCode
        val elapsed = SystemClock.elapsedRealtime() - start
        ConnectivityEvidence(currentVpn(vpn), status == 204, elapsed)
    } finally {
        connection.disconnect()
    }
}
