package com.shield.app // Match your package name

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "ShieldPlugin")
class ShieldPlugin : Plugin() {

    private val VPN_REQUEST_CODE = 0

    @com.getcapacitor.PluginMethod
    fun startVpn(call: PluginCall) {
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            // This triggers the actual Android system VPN permission pop-up!
            activity.startActivityForResult(intent, VPN_REQUEST_CODE)
            call.resolve()
        } else {
            // Permission already granted, start the background VPN service directly
            startVpnService()
            call.resolve()
        }
    }

    @com.getcapacitor.PluginMethod
    fun stopVpn(call: PluginCall) {
        val intent = Intent(activity, ShieldVpnService::class.java)
        activity.stopService(intent)
        call.resolve()
    }

    private fun startVpnService() {
        val intent = Intent(activity, ShieldVpnService::class.java)
        activity.startService(intent)
    }
}
