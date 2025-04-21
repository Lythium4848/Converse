package dev.lythium.converse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import dev.lythium.converse.MainActivity
import dev.lythium.converse.R
import dev.lythium.converse.data.CredentialsStorage
import dev.lythium.converse.manager.LinphoneManager
import dev.lythium.converse.service.ConverseListenerService.Companion.CHANNEL_ID
import org.linphone.core.TransportType
import javax.inject.Inject

@AndroidEntryPoint
class ConverseListenerService : Service() {
    companion object {
        const val CHANNEL_ID = "converse_listener_service"
    }

    @Inject
    lateinit var linphoneManager: LinphoneManager

    @Inject
    lateinit var credentialsStorage: CredentialsStorage

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()

        val username = credentialsStorage.getUsername()
        val password = credentialsStorage.getPassword()
        val domain = credentialsStorage.getDomain()
        val transportType = TransportType.Tcp

        if (username != null && password != null && domain != null) {
            linphoneManager.login(username, password, domain, transportType)
            Log.d("ConverseListenerService", "Logged in")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Listener Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converse Listener")
            .setContentText("Listening for incoming calls")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setColorized(false)
            .build()

        val randId = System.currentTimeMillis() % 10000
        startForeground(randId.toInt(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

}