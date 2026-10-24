package com.shield.app // Match your app's package name

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ShieldVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpnTunnel()
        }
        return START_STICKY
    }

    private fun startVpnTunnel() {
        isRunning = true
        val builder = Builder()
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)   // Route all device traffic into the tunnel
            .addDnsServer("8.8.8.8")
            .setSession("ShieldVPN")
            .setMtu(1500)

        vpnInterface = builder.establish()

        Thread {
            try {
                val descriptor = vpnInterface ?: return@Thread
                val inputStream = FileInputStream(descriptor.fileDescriptor)
                val buffer = ByteBuffer.allocate(32767)

                while (isRunning) {
                    val length = inputStream.read(buffer.array())
                    if (length > 0) {
                        // Packets captured here will be forwarded to your VPS proxy socket
                        buffer.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
    }
}
