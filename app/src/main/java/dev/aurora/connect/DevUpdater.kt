package dev.aurora.connect

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

private const val REPOSITORY = "stub11/aurora-connect"
private const val MAX_APK_BYTES = 100L * 1024 * 1024
private val releaseTag = Regex("dev-([1-9][0-9]*)")
private val apkName = Regex("aurora-connect-0\\.1\\.0-([0-9a-f]{8})\\.apk")
private val checksum = Regex("sha256:([0-9a-f]{64})")

internal data class DevRelease(
    val versionCode: Long,
    val versionName: String,
    val url: String,
    val size: Long,
    val sha256: String,
)

internal class DevUpdater(private val context: Context) {
    private val manager = context.packageManager
    private val packageName = context.packageName

    @Suppress("DEPRECATION")
    private fun installed(): PackageInfo =
        manager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)

    fun check(): DevRelease? {
        val local = installed()
        require(isDevSigned(local))
        val json = open("https://api.github.com/repos/$REPOSITORY/releases?per_page=20") {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= 2 * 1024 * 1024)
                bytes.write(buffer, 0, count)
            }
            JSONArray(bytes.toByteArray().toString(Charsets.UTF_8))
        }
        return newestDevRelease(json, local.longVersionCode)
    }

    fun canInstall(): Boolean = manager.canRequestPackageInstalls()

    fun installSettings(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))

    @Suppress("DEPRECATION")
    fun prepareInstaller(release: DevRelease): Intent {
        val local = installed()
        require(isDevSigned(local) && release.versionCode > local.longVersionCode)

        val directory = File(context.cacheDir, "dev-updates")
        require(directory.isDirectory || directory.mkdirs())
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        val apk = File.createTempFile("aurora-", ".apk", directory)
        var ready = false
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            open(release.url) { input ->
                apk.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= release.size)
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(size == release.size && digest.digest().hex() == release.sha256)
            val archive = requireNotNull(
                manager.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)
            )
            require(archive.packageName == packageName &&
                archive.longVersionCode == release.versionCode &&
                archive.versionName == release.versionName &&
                isDevSigned(archive))
            val uri = FileProvider.getUriForFile(context, "$packageName.updates", apk)
            val installer = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ready = true
            return installer
        } finally {
            if (!ready) apk.delete()
        }
    }

    private fun isDevSigned(info: PackageInfo): Boolean {
        val signer = info.signingInfo?.apkContentsSigners?.singleOrNull() ?: return false
        return MessageDigest.getInstance("SHA-256").digest(signer.toByteArray()).hex() ==
            BuildConfig.DEV_CERT_SHA256
    }
}

internal fun newestDevRelease(releases: JSONArray, installedCode: Long): DevRelease? {
    var newest: DevRelease? = null
    for (index in 0 until releases.length()) {
        val release = releases.getJSONObject(index)
        if (!release.getBoolean("prerelease") || release.getBoolean("draft")) continue
        val tag = release.getString("tag_name")
        val code = releaseTag.matchEntire(tag)?.groupValues?.get(1)?.toLongOrNull() ?: continue
        if (code <= (newest?.versionCode ?: installedCode)) continue
        val assets = release.getJSONArray("assets")
        val apk = (0 until assets.length()).map(assets::getJSONObject)
            .filter { apkName.matches(it.optString("name")) }.singleOrNull() ?: continue
        val name = apk.getString("name")
        val shortSha = apkName.matchEntire(name)!!.groupValues[1]
        val size = apk.optLong("size")
        val digest = checksum.matchEntire(apk.optString("digest"))?.groupValues?.get(1) ?: continue
        val url = "https://github.com/$REPOSITORY/releases/download/$tag/$name"
        if (apk.optString("browser_download_url") != url || size !in 1..MAX_APK_BYTES ||
            apk.optString("state") != "uploaded") continue
        newest = DevRelease(code, "0.1.0-$shortSha", url, size, digest)
    }
    return newest
}

private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

private fun <T> open(address: String, consume: (InputStream) -> T): T {
    var url = URL(address)
    repeat(5) {
        require(url.protocol == "https" && url.userInfo == null &&
            (url.port == -1 || url.port == 443) &&
            url.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com"))
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            if (url.host == "api.github.com") {
                connection.setRequestProperty("Accept", "application/vnd.github+json")
            }
            when (connection.responseCode) {
                200 -> return connection.inputStream.use(consume)
                301, 302, 303, 307, 308 ->
                    url = URL(url, requireNotNull(connection.getHeaderField("Location")))
                else -> error("GitHub request failed")
            }
        } finally {
            connection.disconnect()
        }
    }
    error("Too many redirects")
}
