  package com.moremore.shield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

class ShieldVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.moremore.shield.CONNECT"
        const val ACTION_DISCONNECT = "com.moremore.shield.DISCONNECT"
        const val EXTRA_IP = "server_ip"
        const val EXTRA_PORT = "server_port"
        const val CHANNEL_ID = "shield_vpn"
        const val NOTIFICATION_ID = 1
        @Volatile var isRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_DISCONNECT) stopTunnel()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(stopReceiver, IntentFilter(ACTION_DISCONNECT),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 51820)
                startForeground(NOTIFICATION_ID, buildNotification("Connecting…"))
                startTunnel(ip, port)
                START_STICKY
            }
            ACTION_DISCONNECT -> { stopTunnel(); START_NOT_STICKY }
            else -> START_NOT_STICKY
        }
    }

    private fun startTunnel(ip: String, port: Int) {
        val builder = Builder()
            .setSession("Shield")
            .setMtu(1420)
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            // .addAllowedApplication(packageName) // optional: exclude self

        vpnInterface = builder.establish() ?: run {
            stopSelf()
            return
        }

        // ── WIREGUARD GOES HERE ──
        // The TUN fd below is what you hand to the wireguard-android tunnel:
        //   val tunnel = GoBackend(this)
        //   tunnel.setState(...)
        // For now this creates a "fake" tunnel — apps route through the TUN
        // but traffic dies here until you add a real backend + server.
        val tunFd = vpnInterface!!.fd

        updateNotification("Connected — $ip:$port")
        isRunning = true
    }

    private fun stopTunnel() {
        vpnInterface?.close()
        vpnInterface = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission from system settings
        stopTunnel()
        super.onRevoke()
    }

    // ── Notification helpers (foreground service requirement) ──

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Shield VPN",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(ACTION_DISCONNECT).setPackage(packageName)
        val stopPending = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shield VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // replace with your icon
            .addAction(0, "Disconnect", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
