package com.shield.app

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Registers your VPN plugin so the web UI can talk to Android
        registerPlugin(ShieldPlugin::class.java)
    }
}
