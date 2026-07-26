package com.example.test_ai_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: this is what swaps the launch-window theme for
        // Theme.SecureVault. The system splash is not held open — the splash *route*
        // owns bootstrap, and keeping both would show the mark twice.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Light is pinned rather than following the system: the designs are
            // light-only, and the splash owns its dark tokens explicitly. This is the
            // one line to change when a dark variant is designed.
            AppTheme(darkTheme = false) {
                // Surface, not Scaffold: the splash draws its own full-bleed gradient
                // under the system bars, so the app shell must not inset it. Screens
                // that need insets apply them themselves.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
