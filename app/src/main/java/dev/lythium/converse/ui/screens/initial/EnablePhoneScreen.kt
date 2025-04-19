package dev.lythium.converse.ui.screens.initial

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.lythium.converse.ui.viewmodel.LinphoneViewModel

@Composable
fun EnablePhoneScreen(
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinphoneViewModel = hiltViewModel()
) {
    Column (
        modifier = modifier
            .padding(24.dp)
    ) {
        Text(
            text = "We need to setup your phone to make and receive calls.",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A popup will appear asking you to enable the converse app to make and receive calls. This is required for the app to work properly.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Once you have enabled the phone account, return to the app to continue.",
            style = MaterialTheme.typography.bodyLarge
        )


        var enableButtonClicked by remember { mutableStateOf(false) }
        var phoneAccountHandle by remember { mutableStateOf(viewModel.registerPhoneAccount()) }
        var showFailedText by remember { mutableStateOf(false) }

        Spacer(modifier = Modifier.height(16.dp))

        if (showFailedText) {
            Text(
                text = "Failed to enable phone account. Please try again.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (enableButtonClicked) {
                    if (viewModel.isPhoneAccountEnabled(phoneAccountHandle)) {
                        onSuccess()
                    } else {
                        enableButtonClicked = false
                        showFailedText = true
                    }

                    return@Button
                }

                Log.d("EnablePhoneScreen", "Button clicked")
                viewModel.enablePhoneAccount(phoneAccountHandle)

                enableButtonClicked = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(enableButtonClicked
                .takeIf { it }?.let { "Continue" } ?: "Enable Phone Account"
            )
        }
    }
}
