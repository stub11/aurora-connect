package dev.aurora.connect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService

class AuroraVpnService : VpnService() {
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  val manager = getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("vpn", "VPN", NotificationManager.IMPORTANCE_LOW))
  startForeground(1, Notification.Builder(this, "vpn").setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("Aurora Connect").setContentText("Туннель не настроен").build())
  // No routes or TUN until real backend and consent are implemented.
  stopForeground(STOP_FOREGROUND_REMOVE)
  stopSelf(startId)
  return START_NOT_STICKY
 }
}
