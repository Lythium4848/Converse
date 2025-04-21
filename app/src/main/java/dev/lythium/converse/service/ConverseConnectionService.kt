package dev.lythium.converse.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import dev.lythium.converse.MainActivity
import dev.lythium.converse.R
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConverseConnection @Inject constructor(
    private val core: Core
) : Connection() {
    companion object {
        const val LOG_TAG = "ConverseConnection"
    }

    private var connectionServiceCallback: ConverseConnectionService? = null

    fun setConnectionServiceCallback(callback: ConverseConnectionService) {
        connectionServiceCallback = callback
    }

    fun stopService() {
        connectionServiceCallback?.stopForegroundService()
    }

    fun onLinphoneCallEnded() {
        Log.d(LOG_TAG, "Linphone call ended")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
        stopService()
    }

    override fun onStateChanged(state: Int) {
        Log.d(LOG_TAG, "State changed: $state")

        super.onStateChanged(state)

        if (state == STATE_DISCONNECTED){
            connectionServiceCallback = null
        }
    }

    override fun onAnswer() {
        super.onAnswer()
        setActive()
        core.currentCall?.accept()
        Log.d(LOG_TAG, "Call answered")
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.d(LOG_TAG, "Call disconnected")
        core.currentCall?.terminate()
        stopService()
        destroy()
    }

    override fun onReject() {
        super.onReject()
        Log.d(LOG_TAG, "Call rejected")
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        core.currentCall?.terminate()
        stopService()
        destroy()
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        Log.d("ConverseConnection", "Mute state changed: $isMuted")
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
        const val LOG_TAG = "ConverseConnectionService"
        private const val NOTIFICATION_ID = 2034758
        private const val CHANNEL_ID = "converse_in_call_service"
        private const val CHANNEL_NAME = "In Call Service"
    }

    override fun onDestroy() {
        super.onDestroy()
        converseConnection.destroy()
        Log.d(LOG_TAG, "ConverseConnectionService destroyed")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "ConverseConnectionService created!!!!!!!!")
    }

    @Inject
    lateinit var converseConnection: ConverseConnection

    @Inject
    lateinit var core: Core

    @Inject
    lateinit var factory: Factory

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("Call in progress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setColorized(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        Log.d(LOG_TAG, "Foreground service started with ID: $NOTIFICATION_ID")
    }

    fun stopForegroundService() {
        Log.d(LOG_TAG, "Stopping foreground service")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(LOG_TAG, "onCreateIncomingConnection")

        startForegroundService()
        converseConnection.setConnectionServiceCallback(this)

        converseConnection.setCallerDisplayName(
            request?.address.toString().removePrefix("sip:"),
            TelecomManager.PRESENTATION_ALLOWED
        )

        converseConnection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        converseConnection.audioModeIsVoip = true
        converseConnection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        converseConnection.connectionCapabilities = Connection.CAPABILITY_MUTE or Connection.CAPABILITY_HOLD

        converseConnection.setRinging()

        return converseConnection
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(LOG_TAG, "onCreateOutgoingConnection")

        startForegroundService()
        converseConnection.setConnectionServiceCallback(this)

        if (request?.address == null) {
            Log.e(LOG_TAG, "Request address is null")
            converseConnection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
            return converseConnection
        }

        var addressString = request.address.toString().removePrefix("tel:")
        addressString = "sip:$addressString@pbx.hrzn.network"
        Log.d(LOG_TAG, "Dialing address: $addressString")

        val remoteAddress = factory.createAddress(addressString)
        if (remoteAddress == null) {
            Log.e(LOG_TAG, "Failed to create remote address")
            converseConnection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
            return converseConnection
        }

        val params = core.createCallParams(null)
        if (params == null) {
            Log.e(LOG_TAG, "Failed to create call params")
            converseConnection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
            return converseConnection
        }

        params.mediaEncryption = MediaEncryption.None
        core.inviteAddressWithParams(remoteAddress, params)

        converseConnection.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        converseConnection.audioModeIsVoip = true
        converseConnection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        converseConnection.connectionCapabilities = Connection.CAPABILITY_HOLD or Connection.CAPABILITY_MUTE

        converseConnection.setRinging()

        return converseConnection
    }

}