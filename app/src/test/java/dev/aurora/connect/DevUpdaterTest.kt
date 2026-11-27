package dev.aurora.connect

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevUpdaterTest {
    @Test
    fun selectsHighestNewerPublishedDevVersion() {
        val releases = JSONArray().put(release(34)).put(release(36)).put(release(35))
        assertEquals(36L, newestDevRelease(releases, 33)?.versionCode)
        assertNull(newestDevRelease(releases, 36))
    }

    @Test
    fun rejectsStableDraftAndUntrustedAssets() {
        val releases = JSONArray()
            .put(release(39).put("prerelease", false))
            .put(release(38).put("draft", true))
            .put(release(37, url = "https://github.com/other/aurora-connect/download"))
            .put(release(36, digest = "sha256:invalid"))
        assertNull(newestDevRelease(releases, 35))
    }

    private fun release(
        code: Long,
        name: String = "aurora-connect-0.1.0-0123abcd.apk",
        url: String = "https://github.com/stub11/aurora-connect/releases/download/dev-$code/$name",
        digest: String = "sha256:" + "a".repeat(64),
    ): JSONObject = JSONObject()
        .put("tag_name", "dev-$code")
        .put("prerelease", true)
        .put("draft", false)
        .put("assets", JSONArray().put(JSONObject()
            .put("name", name)
            .put("browser_download_url", url)
            .put("digest", digest)
            .put("size", 1024)
            .put("state", "uploaded")))
}
