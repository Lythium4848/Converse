package dev.lythium.converse.ui.screens.initial

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class StartupPage {
    Welcome,
    Permissions,
    Login
}

@SuppressLint("InlinedApi")
private val REQUIRED_PERMISSIONS = arrayOf(
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.READ_PHONE_NUMBERS,
    android.Manifest.permission.ANSWER_PHONE_CALLS,
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL,
    android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
    android.Manifest.permission.MANAGE_OWN_CALLS,
    android.Manifest.permission.POST_NOTIFICATIONS
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
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val allPermissionsGranted by viewModel.allPermissionsGranted.collectAsState()
    val context = LocalContext.current

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = REQUIRED_PERMISSIONS.toList()
    ) { permissionsResult ->
        viewModel.onAllPermissionsResult(permissionsResult)
    }

    Log.d("StartupScreen", "Current page: $currentPage")

    LaunchedEffect(Unit) {
        if (currentPage == StartupPage.Permissions) {
            Log.d("StartupScreen", "Requesting permissions")
            multiplePermissionsState.launchMultiplePermissionRequest()
            if (allPermissionsGranted) {
                viewModel.navigateTo(StartupPage.Login)
            }
        }
    }

    when (currentPage) {
        StartupPage.Welcome ->  WelcomeScreen({ viewModel.navigateTo(StartupPage.Permissions) }, modifier)
        StartupPage.Permissions -> PermissionsScreen(
            permissions = REQUIRED_PERMISSIONS,
            onGrantPermissions = { multiplePermissionsState.launchMultiplePermissionRequest() },
            shouldShowRationale = multiplePermissionsState.shouldShowRationale,
            modifier = modifier
        )
        StartupPage.Login -> LoginScreen(
            onLoginSuccess = TODO(),
            modifier = modifier
        )
    }
}