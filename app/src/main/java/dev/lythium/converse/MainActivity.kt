package dev.lythium.converse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.lythium.converse.data.AppDataStoreRepository
import dev.lythium.converse.service.ConverseListenerService
import dev.lythium.converse.ui.screens.initial.StartupScreen
import dev.lythium.converse.ui.screens.initial.WelcomeScreen
import dev.lythium.converse.ui.theme.ConverseTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appDataStoreRepository: AppDataStoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            var appFullySetup by remember { mutableStateOf(false) }
            var appReady by remember { mutableStateOf(false) }

            LaunchedEffect(true) {
                scope.launch {
                    appDataStoreRepository.isAppFullySetup.collectLatest { setup ->
                        appReady = true
                        appFullySetup = setup
                        Log.d("MainActivity", "App fully setup: $setup")
                    }
                }
            }

            splashScreen.setKeepOnScreenCondition { !appReady }

            ConverseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainApp(innerPadding, appFullySetup)
                }
            }
        }

        if (!ServiceUtils.isServiceRunning(this, ConverseListenerService::class.java)) {
            Log.d("MainActivity", "ConverseListenerService is not running. Starting it now.")
            val serviceIntent = Intent(this, ConverseListenerService::class.java)
            startForegroundService(serviceIntent)
        } else {
            Log.d("MainActivity", "ConverseListenerService is already running.")
        }
    }
}

@Composable
fun MainApp(
    innerPadding: PaddingValues,
    appFullySetup: Boolean
) {
    if (appFullySetup) {
        Text("App is fully setup", modifier = Modifier.padding(innerPadding))
    } else {
        StartupScreen(modifier = Modifier.padding(innerPadding))
    }
}