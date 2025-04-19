package dev.lythium.converse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.lythium.converse.MainActivity
import dev.lythium.converse.R
import org.linphone.core.AudioDevice
import org.linphone.core.Core
import javax.inject.Inject

class ConverseConnection @Inject constructor(
    private val core: Core
) : Connection() {
    override fun onAnswer() {
        super.onAnswer()

        setActive()

        core.currentCall?.accept()
        core.isMicEnabled = true

        val audioDevice =
            core.audioDevices.find { it.hasCapability(AudioDevice.Capabilities.CapabilityRecord) }

        if (audioDevice != null) {
            core.currentCall?.inputAudioDevice = audioDevice
        } else {
            Log.d("ConverseConnection", "No audio device with recording capabilities found")
        }
    }

    override fun onDisconnect() {
        super.onDisconnect()
        core.currentCall?.terminate()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onReject() {
        super.onReject()
        core.currentCall?.terminate()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        core.isMicEnabled = !isMuted
    }

    override fun onPlayDtmfTone(c: Char) {
        super.onPlayDtmfTone(c)
        core.currentCall?.sendDtmf(c)
    }

    override fun onHold() {
        super.onHold()
        core.currentCall?.pause()
    }

    override fun onUnhold() {
        super.onUnhold()
        core.currentCall?.resume()
    }
}

@AndroidEntryPoint
class ConverseConnectionService : ConnectionService() {
    companion object {
        const val FOREGROUND_SERVICE_ID = 1
        const val LOG_TAG = "ConverseConnectionService"
    }

    @Inject
    lateinit var converseConnection: ConverseConnection

    private fun startForegroundService() {
        val channelId = "converse_in_call_service"
        val channelName = "In Call Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "converse_in_call_service")
            .setContentTitle("In Call Service")
            .setContentText("Service Is Running")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val randId = System.currentTimeMillis() % 10000
        startForeground(randId.toInt(), notification)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(LOG_TAG, "onCreateIncomingConnection")

        converseConnection.setCallerDisplayName(
            request?.address.toString().removePrefix("sip:"),
            TelecomManager.PRESENTATION_ALLOWED
        )

        converseConnection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        converseConnection.audioModeIsVoip = true
        converseConnection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        converseConnection.connectionCapabilities = Connection.CAPABILITY_HOLD + Connection.CAPABILITY_MUTE

        converseConnection.setRinging()

        startForegroundService()

        return converseConnection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(LOG_TAG, "onCreateOutgoingConnection")

        converseConnection.setDialing()

        return converseConnection
    }
}