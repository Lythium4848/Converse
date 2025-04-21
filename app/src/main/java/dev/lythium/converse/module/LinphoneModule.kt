package dev.lythium.converse.module

import android.Manifest
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lythium.converse.data.CredentialsStorage
import dev.lythium.converse.manager.PhoneAccountManager
import dev.lythium.converse.service.ConverseConnection
import dev.lythium.converse.service.ConverseConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LinphoneModule {
    @Provides
    @Singleton
    fun provideLinphoneFactory(): Factory {
        val factory = Factory.instance()
//        factory.enableLogCollection(LogCollectionState.Enabled)
//        factory.setLoggerDomain("dev.lythium.converse")
//        factory.enableLogcatLogs(true)
//        factory.loggingService.setLogLevel(LogLevel.Debug)

        return factory
    }

    @Provides
    @Singleton
    fun provideLinphoneCore(
        factory: Factory,
        @ApplicationContext context: Context
    ): Core {
        val factory = Factory.instance()
        val core = factory.createCore(null, null, context)
        core.playFile = ""

        return core
    }

    @Provides
    @Singleton
    fun provideLinphoneCoreListener(
        @ApplicationContext context: Context,
        phoneAccountManager: PhoneAccountManager,
        converseConnection: ConverseConnection
    ): LinphoneCoreListener {
        return LinphoneCoreListener(context, phoneAccountManager, converseConnection)
    }

    fun login(
        core: Core,
        coreListener: LinphoneCoreListener,
        username: String,
        password: String,
        domain: String,
        transportType: TransportType
    ) {
        val authInfo =
            Factory
                .instance()
                .createAuthInfo(username, null, password, null, null, domain, null)

        val accountParams = core.createAccountParams()
        val identity = Factory
            .instance()
            .createAddress("sip:$username@$domain")

        val address = Factory
            .instance()
            .createAddress("sip:$username@$domain")

        address?.transport = transportType

        accountParams.identityAddress = identity
        accountParams.serverAddress = address
        accountParams.isRegisterEnabled = true

        val account = core.createAccount(accountParams)

        core.addAuthInfo(authInfo)
        core.addAccount(account)
        core.defaultAccount = account

        core.addListener(coreListener)
        core.start()
    }
}

@Singleton
class LinphoneCoreListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneAccountManager: PhoneAccountManager,
    private val converseConnection: ConverseConnection
) : CoreListenerStub() {
    override fun onAccountRegistrationStateChanged(
        core: Core,
        account: Account,
        state: RegistrationState?,
        message: String
    ) {
        println("Registration state changed: $state - $message")
    }

    @RequiresPermission(allOf = [Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE])
    override fun onCallStateChanged(
        core: Core,
        call: Call,
        state: Call.State?,
        message: String
    ) {
        val telecomManager = context.getSystemService(TELECOM_SERVICE) as TelecomManager

        when (state) {
            Call.State.IncomingReceived -> {
                Log.d("LinphoneModule", "Incoming call received")

                val phoneAccountHandle = phoneAccountManager.getPhoneAccountHandle()

                val address = Uri.fromParts("sip", call.remoteAddress.username, null)
                val extras = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, address)
                }

                if (phoneAccountManager.isPhoneAccountEnabled(phoneAccountHandle)) {
                    try {
                        Log.d("LinphoneModule", "Attempting to push to telecom manager")
                        telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
                    } catch (e: Exception) {
                        Log.e("LinphoneModule", "Failed to push call to telecom manager: ${e.message}")
                    }
                } else {
                    phoneAccountManager.enablePhoneAccount(phoneAccountHandle)
                    Log.d("LinphoneModule", "Phone account not enabled, prompting user")
                }
            }
            Call.State.End -> {
                Log.d("LinphoneModule", "Call ended")
                converseConnection.onLinphoneCallEnded()
            }
            Call.State.Released -> {
                Log.d("LinphoneModule", "Call released")
            }
            Call.State.OutgoingInit -> {
                Log.d("LinphoneModule", "Outgoing call initiated")

                val address = Uri.fromParts("sip", call.remoteAddress.username, null)

                val bundle = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountManager.getPhoneAccountHandle())
                }

                try {
                    telecomManager.placeCall(address, bundle)
                    Log.d("LinphoneModule", "Outgoing call placed")
                } catch(e: Exception) {
                    Log.e("LinphoneModule", "Failed to place outgoing call: ${e.message}")
                }
            }
            Call.State.OutgoingProgress -> {
                Log.d("LinphoneModule", "Outgoing call in progress")
            }
            Call.State.OutgoingRinging -> {
                Log.d("LinphoneModule", "Outgoing call ringing")
            }
            Call.State.Connected -> {
                Log.d("LinphoneModule", "Outgoing call connected!!!!!!")
                converseConnection.setActive()


            }
            Call.State.Error -> {
                Log.d("LinphoneModule", "Call error: $message")
                converseConnection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
                converseConnection.stopService()
                converseConnection.destroy()
            }
            else -> {
                Log.d("LinphoneModule", "Unhandled call state changed: $state")
            }
        }
    }
}