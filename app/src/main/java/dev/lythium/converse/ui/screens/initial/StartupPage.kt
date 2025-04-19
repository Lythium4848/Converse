package dev.lythium.converse.ui.screens.initial

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lythium.converse.ui.viewmodel.LinphoneViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.linphone.core.TransportType
import javax.inject.Inject

enum class StartupPage {
    Welcome,
    Permissions,
    Login,
    EnablePhone,
}

@SuppressLint("InlinedApi")
private val REQUIRED_PERMISSIONS = listOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.READ_PHONE_NUMBERS,
    Manifest.permission.ANSWER_PHONE_CALLS,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL,
    Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
    Manifest.permission.MANAGE_OWN_CALLS,
    Manifest.permission.POST_NOTIFICATIONS
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _currentPage = MutableStateFlow(StartupPage.Welcome)
    val currentPage: StateFlow<StartupPage> = _currentPage

    private val _allPermissionsGranted = MutableStateFlow(false)
    val allPermissionsGranted: StateFlow<Boolean> = _allPermissionsGranted

    fun navigateTo(page: StartupPage) {
        _currentPage.value = page
    }

    fun onAllPermissionsResult(permissionsResult: Map<String, Boolean>) {
        val allGranted = REQUIRED_PERMISSIONS.all { permissionsResult[it] == true }
        _allPermissionsGranted.value = allGranted

        Log.d("StartupViewModel", "All permissions granted: $allGranted")

        for ((permission, granted) in permissionsResult) {
            if (granted) {
                Log.d("StartupViewModel", "Permission granted: $permission")
            } else {
                Log.d("StartupViewModel", "Permission denied: $permission")
            }
        }

        if (allGranted) {
            navigateTo(StartupPage.Login)
        } else {
            Toast.makeText(appContext, "Some required permissions were denied", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartupScreen(
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = viewModel(),
    linphoneViewModel: LinphoneViewModel = hiltViewModel()
) {
    val currentPage by viewModel.currentPage.collectAsState()

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = REQUIRED_PERMISSIONS
    ) { permissionsResult ->
        viewModel.onAllPermissionsResult(permissionsResult)
    }

    LaunchedEffect(Unit) {
        val (username, password, domain) = linphoneViewModel.getCredentials()
        if (username.isNotEmpty() && password.isNotEmpty() && domain.isNotEmpty()) {
            val savedLoginSuccess = linphoneViewModel.login(username, password, domain, TransportType.Tcp)
            if (savedLoginSuccess) {
                viewModel.navigateTo(StartupPage.EnablePhone)
            }
        }
    }

    when (currentPage) {
        StartupPage.Welcome ->  WelcomeScreen({
            if (multiplePermissionsState.allPermissionsGranted) {
                viewModel.navigateTo(StartupPage.Login)
            } else {
                viewModel.navigateTo(StartupPage.Permissions)
            }
        }, modifier)
        StartupPage.Permissions -> PermissionsScreen(
            multiplePermissionsState = multiplePermissionsState,
            modifier = modifier
        )
        StartupPage.Login -> LoginScreen(
            onLoginSuccess = { viewModel.navigateTo(StartupPage.EnablePhone) },
            modifier = modifier
        )
        StartupPage.EnablePhone -> EnablePhoneScreen(
            onSuccess = { Log.d("StartupScreen", "Phone enabled") },
            modifier = modifier
        )
    }
}