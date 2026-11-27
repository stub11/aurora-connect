package dev.aurora.connect

import android.os.Bundle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.net.VpnService
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
 private val backend by lazy { WireGuardBackend.getInstance(applicationContext) }
 private val store by lazy { EncryptedConfigStore(applicationContext) }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme = darkColorScheme()) { Screen() } }
 }
 @Composable private fun Screen() {
  val scope = rememberCoroutineScope()
  var page by remember { mutableIntStateOf(0) }
  val preferences = getSharedPreferences("ui", MODE_PRIVATE)
  var mode by remember { mutableStateOf(runCatching { Mode.valueOf(preferences.getString("mode", "LAN") ?: "LAN") }.getOrDefault(Mode.LAN)) }
  var configured by remember { mutableStateOf(false) }
  var metadata by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var snapshot by remember { mutableStateOf(WireGuardSnapshot(false,0,0,0)) }
  var error by remember { mutableStateOf("") }
  val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  fun importText(text: String) {
   scope.launch {
    busy = true
    try { withContext(Dispatchers.IO) { store.save(text); configMetadata(text) }.also { metadata = it }; configured = true; error = "" }
    catch (_: Exception) { error = "Не удалось импортировать конфигурацию" }
    finally { busy = false }
   }
  }
  fun connect() {
   scope.launch {
    busy = true
    try {
     snapshot = withContext(Dispatchers.IO) {
      backend.connect(parseWireGuardConfig(modeConfig(checkNotNull(store.load()), mode)))
      backend.snapshot()
     }
     error = ""
    }
    catch (_: Exception) { error = "Не удалось запустить VPN" }
    finally { busy = false }
   }
  }
  fun disconnect() {
   scope.launch {
    busy = true
    try {
     snapshot = withContext(Dispatchers.IO) {
      backend.disconnect()
      backend.snapshot()
     }
     error = ""
    } catch (_: Exception) {
     error = "Не удалось остановить VPN"
    } finally {
     busy = false
    }
   }
  }
  val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let { importText(it) } }
  val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
   if (result.resultCode == RESULT_OK) connect() else error = "Разрешение VPN не предоставлено"
  }
  val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
   if (uri != null) scope.launch {
    try {
     val text = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { stream ->
      val bytes = ByteArray(65537); var size = 0
      while (size < bytes.size) { val n = stream.read(bytes, size, bytes.size-size); if (n <= 0) break; size += n }
      require(size <= 65536); String(bytes,0,size,Charsets.UTF_8)
     } ?: error("file") }
     importText(text)
    } catch (_: Exception) { error = "Не удалось прочитать файл" }
   }
  }
  LaunchedEffect(Unit) {
   metadata = withContext(Dispatchers.IO) { runCatching { store.load()?.let(::configMetadata).orEmpty() }.getOrDefault("") }
   configured = metadata.isNotEmpty()
   while (true) {
    snapshot = withContext(Dispatchers.IO) { runCatching { backend.snapshot() }.getOrDefault(WireGuardSnapshot(false,0,0,0)) }
    delay(2000)
   }
  }
  Scaffold(contentWindowInsets = WindowInsets.safeDrawing, bottomBar = {
   NavigationBar { listOf("Главная","Конфигурация","Диагностика").forEachIndexed { index, label ->
    NavigationBarItem(selected = page == index, onClick = { page = index }, icon = { Text((index+1).toString()) }, label = { Text(label) })
   } }
  }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Aurora Connect", style = MaterialTheme.typography.headlineMedium)
    when(page) {
     0 -> {
      Text("Режим подключения")
      Mode.entries.forEach { option -> Row { RadioButton(selected = mode == option, enabled = !busy && !snapshot.up, onClick = { mode = option; preferences.edit().putString("mode", option.name).apply() }); Text(when(option) { Mode.LAN -> "LAN"; Mode.FULL -> "Full tunnel"; Mode.SPLIT -> "Split tunnel" }) } }
      Text("RTT: —")
      Text(when {
       !snapshot.up -> "DNS: —"
       snapshot.dnsServers.isEmpty() -> "DNS туннеля: не задан"
       else -> "DNS туннеля: ${snapshot.dnsServers.joinToString()}"
      })
      Text(metadata.lineSequence().firstOrNull { it.startsWith("Endpoint:") } ?: "Endpoint: —")
      Text(if(snapshot.up) "Туннель поднят; связность не проверена" else "Отключено")
      Button(enabled = !busy && (configured || snapshot.up), onClick = {
       if(snapshot.up) disconnect()
       else { val intent = VpnService.prepare(this@MainActivity); if(intent == null) connect() else consent.launch(intent) }
      }) { Text(if(snapshot.up) "Отключить" else if(configured) "Подключить" else "Нет конфигурации") }
      Text("Handshake: " + if(snapshot.handshakeEpochMs > 0) java.util.Date(snapshot.handshakeEpochMs).toString() else "—")
      Text("RX: ${snapshot.rx} Б / TX: ${snapshot.tx} Б")
      Text("Реальный трафик: не проверено")
     }
     1 -> {
      Text("Конфигурация")
      Text(if(configured) "Конфигурация сохранена в зашифрованном виде" else "Нет конфигурации")
      Button(enabled = !busy && !snapshot.up, onClick = { clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.let { importText(it) } }) { Text("Вставить из буфера") }
      Button(enabled = !busy && !snapshot.up, onClick = { file.launch(arrayOf("*/*")) }) { Text("Импорт из файла") }
      Button(enabled = !busy && !snapshot.up, onClick = { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Сканируйте конфигурацию WireGuard").setBeepEnabled(false)) }) { Text("Сканировать QR") }
      if (configured) Text(metadata)
     }
     else -> {
      Text("Диагностика")
      DevUpdatePanel()
      Text(if (snapshot.up) "Туннель поднят" else "Туннель отключён")
      Text(if (snapshot.handshakeEpochMs > 0) "WireGuard: есть handshake" else "WireGuard: handshake пока отсутствует")
      Text("RX: ${snapshot.rx} Б / TX: ${snapshot.tx} Б")
      Text("Связность, внешний IP, DNS-запросы и IPv6: не проверены")
      Button(onClick = {
       val report = buildString {
        appendLine("Aurora Connect ${BuildConfig.VERSION_NAME} (сборка ${BuildConfig.VERSION_CODE})")
        appendLine("Android ${android.os.Build.VERSION.RELEASE}")
        appendLine("Режим: $mode")
        appendLine("Туннель поднят: ${snapshot.up}")
        appendLine("Handshake: ${snapshot.handshakeEpochMs > 0}")
        appendLine("RX: ${snapshot.rx} Б / TX: ${snapshot.tx} Б")
        appendLine("DNS-серверов в туннеле: ${snapshot.dnsServers.size}")
        append("Связность, внешний IP, DNS-запросы и IPv6: не проверены")
       }
       clipboard.setPrimaryClip(ClipData.newPlainText("Aurora Connect", report))
      }) { Text("Скопировать отчёт") }
     }
    }
    if(error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
   }
  }
 }
}
