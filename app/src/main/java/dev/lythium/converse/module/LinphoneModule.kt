package dev.lythium.converse.module

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lythium.converse.data.CredentialsStorage
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
        @ApplicationContext context: Context
    ): LinphoneCoreListener {
        return LinphoneCoreListener(context)
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
    @ApplicationContext private val context: Context
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
        println("Call state changed: $state - $message")
    }
}