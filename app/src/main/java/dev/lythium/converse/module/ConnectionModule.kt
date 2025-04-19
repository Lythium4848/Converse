package dev.lythium.converse.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dev.lythium.converse.service.ConverseConnection
import org.linphone.core.Core

@Module
@InstallIn(ServiceComponent::class)
object ConnectionModule {
    @Provides
    fun provideConverseConnection(core: Core): ConverseConnection {
        return ConverseConnection(core)
    }
}
