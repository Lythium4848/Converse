package dev.lythium.converse.ui.screens.initial

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.lythium.converse.ui.elements.PasswordTextField
import dev.lythium.converse.ui.viewmodel.LinphoneViewModel
import dev.lythium.converse.ui.viewmodel.LinphoneViewModel.LoginState
import kotlinx.coroutines.launch
import org.linphone.core.TransportType

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LinphoneViewModel = hiltViewModel()
) {
    Column (
        modifier = modifier
            .padding(24.dp)
    ) {
        Text(
            text = "We need to login to your SIP account to continue.",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "If you don't know these details, check your SIP providers website or documentation.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        val loginState by viewModel.loginState

        var username by remember { mutableStateOf("") }
        var usernameIsError by remember { mutableStateOf(false) }

        var password by remember { mutableStateOf("") }
        var passwordIsError by remember { mutableStateOf(false) }

        var domain by remember { mutableStateOf("") }
        var domainIsError by remember { mutableStateOf(false) }

        val transportType = TransportType.Tcp

        val scope = rememberCoroutineScope()

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = {
                Text("Username")
            },
            modifier = Modifier
                .fillMaxWidth(),
            isError = usernameIsError
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text("Password")
            },
            modifier = Modifier
                .fillMaxWidth(),
            isError = passwordIsError
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = {
                Text("Domain")
            },
            modifier = Modifier
                .fillMaxWidth(),
            isError = domainIsError
        )

        Spacer(modifier = Modifier.height(8.dp))


        Button(
            onClick = {
                usernameIsError = username.isEmpty()
                passwordIsError = password.isEmpty()
                domainIsError = domain.isEmpty()

                if (!usernameIsError && !passwordIsError && !domainIsError) {
                    scope.launch {
                        viewModel.login(
                            username = username,
                            password = password,
                            domain = domain,
                            transportType = transportType
                        )

                        if (loginState == LoginState.Success) {
                            onLoginSuccess()
                        }
                    }

                    return@Button
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Login")
        }

        when (loginState) {
            LoginState.Idle -> Text("Idle")
            LoginState.Loading -> Text("Logging in...")
            LoginState.Success -> Text("Login successful!")
            is LoginState.Error -> Text("Login failed: ${(loginState as LoginState.Error).message}")
        }
    }
}