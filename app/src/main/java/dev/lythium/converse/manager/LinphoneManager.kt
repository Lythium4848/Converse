package dev.lythium.converse.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lythium.converse.data.CredentialsStorage
import dev.lythium.converse.module.LinphoneCoreListener
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.TransportType
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LinphoneManager @Inject constructor(
    private val factory: Factory,
    private val core: Core,
    private val coreListener: LinphoneCoreListener,
    private val credentialsStorage: CredentialsStorage,
    @ApplicationContext private val appContext: Context
) {
    init {
        core.addListener(coreListener)
        core.start()
    }

    fun login(
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

        credentialsStorage.saveCredentials(
            username,
            password,
            domain
        )
    }
}