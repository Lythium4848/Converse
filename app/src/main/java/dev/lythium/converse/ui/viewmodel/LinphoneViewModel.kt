package dev.lythium.converse.ui.viewmodel

import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lythium.converse.manager.LinphoneManager
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dev.lythium.converse.data.CredentialsStorage
import dev.lythium.converse.manager.PhoneAccountManager
import kotlinx.coroutines.launch
import org.linphone.core.TransportType

@HiltViewModel
class LinphoneViewModel @Inject constructor(
    private val linphoneManager: LinphoneManager,
    private val credentialsStorage: CredentialsStorage,
    private val phoneAccountManager: PhoneAccountManager
) : ViewModel() {
    var loginState: MutableState<LoginState> = mutableStateOf<LoginState>(LoginState.Idle)
        private set

    fun getCredentials(): Triple<String, String, String> {
        val username = credentialsStorage.getUsername() ?: ""
        val password = credentialsStorage.getPassword() ?: ""
        val domain = credentialsStorage.getDomain() ?: ""

        return Triple(username, password, domain)
    }

    suspend fun login(
        username: String,
        password: String,
        domain: String,
        transportType: TransportType,
    ): Boolean {
        return try {
            loginState.value = LoginState.Loading
            linphoneManager.login(username, password, domain, transportType)
            loginState.value = LoginState.Success
            true
        } catch (e: Exception) {
            loginState.value = LoginState.Error(e.message ?: "Unknown error")
            false
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    fun registerPhoneAccount(): PhoneAccountHandle {
        val phoneAccountHandle = phoneAccountManager.registerPhoneAccount()
        return phoneAccountHandle
    }

    fun enablePhoneAccount(phoneAccountHandle: PhoneAccountHandle) {
        phoneAccountManager.enablePhoneAccount(phoneAccountHandle)
    }

    fun isPhoneAccountEnabled(phoneAccountHandle: PhoneAccountHandle): Boolean {
        return phoneAccountManager.isPhoneAccountEnabled(phoneAccountHandle)
    }
}