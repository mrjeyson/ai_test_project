package com.example.test_ai_project.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.app.navigation.RootNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: this is what swaps the launch-window theme for
        // Theme.SecureVault. The window is not held open past the first frame — the
        // launch mark is the whole of the launch experience, and login draws straight
        // after it.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // AppTheme has a single light scheme and does not follow the system: the
            // designs are light-only, and the screens that are dark — the viewfinder —
            // own their tokens explicitly.
            AppTheme {
                // Surface, not Scaffold: the viewfinder draws full-bleed under the
                // system bars, so the app shell must not inset it. Screens that need
                // insets apply them themselves.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RootNavHost()
                }
            }
        }
    }
}




















