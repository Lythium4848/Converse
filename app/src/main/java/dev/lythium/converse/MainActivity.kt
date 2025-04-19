package dev.lythium.converse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import dev.lythium.converse.service.ConverseListenerService
import dev.lythium.converse.ui.screens.initial.StartupScreen
import dev.lythium.converse.ui.screens.initial.WelcomeScreen
import dev.lythium.converse.ui.theme.ConverseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConverseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartupScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ConverseTheme {
        Greeting("Android")
    }
}