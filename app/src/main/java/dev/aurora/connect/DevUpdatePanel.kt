package dev.aurora.connect

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DevUpdatePanel() {
    val context = LocalContext.current
    val updater = remember(context) { DevUpdater(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<DevRelease?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Text("Тестовая версия ${BuildConfig.VERSION_NAME} · сборка ${BuildConfig.VERSION_CODE}")
    Button(enabled = !busy, onClick = {
        scope.launch {
            busy = true
            release = null
            status = "Проверяем обновления…"
            try {
                val available = withContext(Dispatchers.IO) { updater.check() }
                release = available
                status = if (available == null) "Новых тестовых сборок нет"
                    else "Доступна тестовая сборка ${available.versionCode} (${available.versionName})"
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                status = "Не удалось проверить обновления. Попробуйте ещё раз."
            } finally {
                busy = false
            }
        }
    }) { Text("Проверить обновления") }

    release?.let { available ->
        Button(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    if (updater.canInstall()) {
                        status = "Скачиваем и проверяем APK…"
                        val installer = withContext(Dispatchers.IO) { updater.prepareInstaller(available) }
                        context.startActivity(installer)
                        status = "Подтвердите обновление в окне Android"
                    } else {
                        context.startActivity(updater.installSettings())
                        status = "Разрешите установку для Aurora Connect, вернитесь и нажмите кнопку ещё раз"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    status = "Не удалось скачать, проверить или открыть APK"
                } finally {
                    busy = false
                }
            }
        }) { Text("Скачать и установить") }
    }
    if (status.isNotEmpty()) Text(status)
}
