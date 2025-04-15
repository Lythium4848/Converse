package dev.lythium.converse.data

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsStorage @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    private companion object {
        const val KEY_USERNAME = "converse_username"
        const val KEY_PASSWORD = "converse_password"
        const val KEY_DOMAIN = "converse_domain"
    }

    fun saveCredentials(
        username: String,
        password: String,
        domain: String
    ) {
        sharedPreferences.edit {
            putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_DOMAIN, domain)
        }
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getPassword(): String? {
        return sharedPreferences.getString(KEY_PASSWORD, null)
    }

    fun getDomain(): String? {
        return sharedPreferences.getString(KEY_DOMAIN, null)
    }

    fun clearCredentials() {
        sharedPreferences.edit {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            remove(KEY_DOMAIN)
        }
    }
}