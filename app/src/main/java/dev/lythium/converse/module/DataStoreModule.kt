package dev.lythium.converse.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.datastore.preferences.core.Preferences

private const val APP_DATASTORE_NAME = "converse_app_data"

@Module
@InstallIn(SingletonComponent::class)
class DataStoreModule {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_DATASTORE_NAME)

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}