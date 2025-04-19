package dev.lythium.converse.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lythium.converse.R
import dev.lythium.converse.service.ConverseConnectionService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneAccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPhoneAccountHandle(): PhoneAccountHandle {
        val componentName = ComponentName(context, ConverseConnectionService::class.java)
        return PhoneAccountHandle(componentName, "Converse")
    }

    fun registerPhoneAccount(): PhoneAccountHandle {
        val phoneAccountHandle = getPhoneAccountHandle()

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "Converse")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .build()

        telecomManager.registerPhoneAccount(phoneAccount)

        return phoneAccountHandle
    }

    fun enablePhoneAccount(
        phoneAccountHandle: PhoneAccountHandle
    ) {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isPhoneAccountEnabled(
        phoneAccountHandle: PhoneAccountHandle
    ): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle)
        return phoneAccount != null && phoneAccount.isEnabled
    }
}