package dev.lythium.converse.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataStoreRepository @Inject constructor(
    private val appDataStore: DataStore<Preferences>
) {
    private companion object {
        val KEY_IS_APP_FULLY_SETUP = booleanPreferencesKey("is_app_fully_setup")
    }

    suspend fun setAppFullySetup(isSetup: Boolean) {
        appDataStore.edit { preferences ->
            preferences[KEY_IS_APP_FULLY_SETUP] = isSetup
        }
    }

    val isAppFullySetup: Flow<Boolean> = appDataStore.data.map { preferences ->
        preferences[KEY_IS_APP_FULLY_SETUP] == true
    }
}