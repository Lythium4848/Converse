package dev.lythium.converse.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.lythium.converse.service.ConverseListenerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("BootReceiver", "onReceive called. Action: ${intent?.action}")
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            Log.d("BootReceiver", "BOOT_COMPLETED intent received.")
            val serviceIntent = Intent(context, ConverseListenerService::class.java)
            try {
                context.startForegroundService(serviceIntent)
                Log.d("BootReceiver", "ConverseListenerService started successfully.")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting service: ${e.message}")
            }
        }
    }
}