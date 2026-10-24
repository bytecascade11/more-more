package com.shield.app

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
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0) // Route all device traffic through tunnel
                .addDnsServer("8.8.8.8")
                .setSession("Litron Shield")
                .setMtu(1500)

            vpnInterface = builder.establish()

            // Background thread to keep the virtual interface alive and reading packets
            Thread {
                try {
                    val descriptor = vpnInterface
                    if (descriptor != null) {
                        val inputStream = FileInputStream(descriptor.fileDescriptor)
                        val buffer = ByteBuffer.allocate(32767)

                        while (isRunning) {
                            val length = inputStream.read(buffer.array())
                            if (length > 0) {
                                // Packet captured successfully
                                buffer.clear()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null
    }
}
