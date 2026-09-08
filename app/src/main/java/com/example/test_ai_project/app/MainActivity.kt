package com.example.test_ai_project.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.ThemeMode
import com.example.test_ai_project.resource.theme.ThemeService
import com.example.test_ai_project.app.navigation.RootNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Injected into the activity rather than read by a ViewModel: the theme is decided
     * above the whole navigation graph, and its value is already in memory by the time the
     * graph is built, so there is nothing to load and nothing to hold state for.
     */
    @Inject
    lateinit var themeService: ThemeService

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: this is what swaps the launch-window theme for
        // Theme.SecureVault. The window is not held open past the first frame — the
        // launch mark is the whole of the launch experience, and login draws straight
        // after it.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by themeService.mode.collectAsStateWithLifecycle()
            val darkTheme = mode == ThemeMode.Dark

            // Re-applied on every change, because the bar *icons* are not part of the
            // Compose tree: left alone, a switch to the dark scheme would leave dark
            // glyphs sitting on the dark background they were drawn to contrast with.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle(darkTheme),
                    navigationBarStyle = systemBarStyle(darkTheme),
                )
            }

            AppTheme(darkTheme = darkTheme) {
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

    /**
     * Transparent bars either way — the app draws its own background under them. Only the
     * glyph colour differs, which is what the light/dark distinction selects.
     */
    private fun systemBarStyle(darkTheme: Boolean): SystemBarStyle = if (darkTheme) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }
}









