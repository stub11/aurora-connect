package dev.aurora.connect

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var downloadPercent by remember { mutableStateOf<Int?>(null) }
    var verifying by remember { mutableStateOf(false) }

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
                        status = "Скачивание APK…"
                        downloadPercent = 0
                        val installer = withContext(Dispatchers.IO) {
                            updater.prepareInstaller(
                                available,
                                onDownload = { percent ->
                                    withContext(Dispatchers.Main) { downloadPercent = percent }
                                },
                                onVerify = {
                                    withContext(Dispatchers.Main) {
                                        downloadPercent = null
                                        verifying = true
                                        status = "Проверка APK…"
                                    }
                                },
                            )
                        }
                        context.startActivity(installer)
                        status = "Подтвердите обновление. Ход установки покажет Android."
                    } else {
                        context.startActivity(updater.installSettings())
                        status = "Разрешите установку для Aurora Connect, вернитесь и нажмите кнопку ещё раз"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: UpdateFailure) {
                    status = "${e.messageId}. Не удалось продолжить. Попробуйте ещё раз."
                } catch (_: Exception) {
                    status = "Не удалось открыть установщик или настройки Android"
                } finally {
                    downloadPercent = null
                    verifying = false
                    busy = false
                }
            }
        }) { Text("Скачать и установить") }
    }
    if (status.isNotEmpty()) Text(status)
    val percent = downloadPercent
    if (percent != null) {
        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
        Text("$percent%")
    } else if (verifying) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
