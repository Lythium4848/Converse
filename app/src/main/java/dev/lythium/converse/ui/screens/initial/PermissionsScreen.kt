package dev.lythium.converse.ui.screens.initial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    multiplePermissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
            .padding(24.dp)
    ) {
        Text(
            text = "Converse requires some permissions to function properly.",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please allow the permissions to ensure a smooth experience. Give Converse access to Phone, Notifications and Microphone.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
            Text("Request permissions")
        }
    }
}