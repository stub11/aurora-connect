package dev.aurora.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme { Surface { ConnectScreen() } } }
 }
}
@Composable
fun ConnectScreen(backend: TunnelBackend = remember { UnconfiguredBackend() }) {
 var mode by remember { mutableStateOf(Mode.LAN) }
 var status by remember { mutableStateOf(TunnelStatus()) }
 Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Text("Aurora Connect", style = MaterialTheme.typography.headlineMedium)
  Text("Прототип: реальный туннель не настроен")
  Mode.entries.forEach { item ->
   Row { RadioButton(selected = mode == item, onClick = { mode = item }); Text(when(item) { Mode.LAN -> "LAN"; Mode.FULL -> "Full tunnel"; Mode.SPLIT -> "Split tunnel" }) }
  }
  Button(onClick = { status = if (status.phase == Phase.CONNECTED || status.phase == Phase.CONNECTING) backend.disconnect() else backend.connect(mode) }) {
   Text(if (status.phase == Phase.CONNECTED || status.phase == Phase.CONNECTING) "Отключить" else "Подключить")
  }
  Text(if (status.phase == Phase.FAILED) "Нет настроенного транспорта" else "Статус: отключено")
  Text("Handshake: не проверено")
  Text("Реальный трафик: не проверено")
  Text("RTT: —")
  Text("DNS: —")
 }
}
