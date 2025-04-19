package dev.lythium.converse.module

import android.Manifest
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lythium.converse.data.CredentialsStorage
import dev.lythium.converse.manager.PhoneAccountManager
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
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
        return Factory.instance()
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
        phoneAccountManager: PhoneAccountManager
    ): LinphoneCoreListener {
        return LinphoneCoreListener(context, phoneAccountManager)
    }

    fun login(
        factory: Factory,
        core: Core,
        coreListener: LinphoneCoreListener,
        credentialsStorage: CredentialsStorage,
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
    private val phoneAccountManager: PhoneAccountManager
) : CoreListenerStub() {
    override fun onAccountRegistrationStateChanged(
        core: Core,
        account: Account,
        state: RegistrationState?,
        message: String
    ) {
        println("Registration state changed: $state - $message")
    }


    @RequiresPermission(Manifest.permission.ANSWER_PHONE_CALLS)
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
                    Log.d("LinphoneModule", "Pushing to telecom manager")
                    telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
                } else {
                    phoneAccountManager.enablePhoneAccount(phoneAccountHandle)
                }
            }
            Call.State.End -> {
                Log.d("LinphoneModule", "Call ended")
                core.currentCall?.terminate()
            }
            Call.State.Released -> {
                Log.d("LinphoneModule", "Call released")
                telecomManager.endCall()
            }
            else -> {
                Log.d("LinphoneModule", "Unhandled call state changed: $state")
            }
        }
    }
}