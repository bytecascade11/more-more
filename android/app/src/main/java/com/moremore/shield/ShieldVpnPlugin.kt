package com.yourname.shield

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "ShieldVpn")
class ShieldVpnPlugin : Plugin() {

    companion object {
        private const val REQ_VPN_CONSENT = 1001
    }

    /** Holds the call while we wait for the Android VPN consent dialog. */
    private var pendingCall: PluginCall? = null
    private var pendingConfig: TunnelConfig? = null

    private data class TunnelConfig(val ip: String, val port: Int)

    @PluginMethod
    fun connect(call: PluginCall) {
        val ip = call.getString("serverIp") ?: return call.reject("serverIp is required")
        val port = call.getInt("serverPort", 51820)!!

        val intent = VpnService.prepare(context)
        if (intent != null) {
            // System will show the "Connection request" dialog
            pendingCall = call
            pendingConfig = TunnelConfig(ip, port)
            startActivityForResult(call, intent, "vpnConsent")
        } else {
            startTunnel(call, TunnelConfig(ip, port))
        }
    }

    @PluginMethod
    fun disconnect(call: PluginCall) {
        val intent = Intent(context, ShieldVpnService::class.java)
            .setAction(ShieldVpnService.ACTION_DISCONNECT)
        context.startService(intent)
        notifyState(false)
        call.resolve()
    }

    @PluginMethod
    fun getStatus(call: PluginCall) {
        val result = JSObject().apply {
            put("connected", ShieldVpnService.isRunning)
        }
        call.resolve(result)
    }

    override fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.handleOnActivityResult(requestCode, resultCode, data)
        val call = pendingCall ?: return
        val config = pendingConfig

        pendingCall = null
        pendingConfig = null

        if (resultCode == Activity.RESULT_OK && config != null) {
            startTunnel(call, config)
        } else {
            call.reject("User denied VPN permission")
        }
    }

    private fun startTunnel(call: PluginCall, config: TunnelConfig) {
        val intent = Intent(context, ShieldVpnService::class.java).apply {
            action = ShieldVpnService.ACTION_CONNECT
            putExtra(ShieldVpnService.EXTRA_IP, config.ip)
            putExtra(ShieldVpnService.EXTRA_PORT, config.port)
        }
        context.startForegroundService(intent)

        notifyState(true)
        call.resolve(JSObject().put("connected", true))
    }

    /** Emits to JS so your Astro UI can react to state changes. */
    private fun notifyState(connected: Boolean) {
        val data = JSObject().put("connected", connected)
        notifyListeners("vpnStateChanged", data)
    }
}
