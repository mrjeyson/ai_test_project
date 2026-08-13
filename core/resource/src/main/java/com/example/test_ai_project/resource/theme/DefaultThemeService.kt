package com.example.test_ai_project.resource.theme

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The theme choice, kept in [SharedPreferences].
 *
 * Not Room, though the app has it: this is a single enum that has to be readable *before*
 * the first frame is composed, and a query through the database layer would hand back its
 * first value one frame late — which the user would see as the app opening light and
 * turning dark. SharedPreferences answers from an in-memory map after the first read, and
 * that read happens once, here, while the graph is being built.
 *
 * An unrecognised stored value falls back to the default rather than throwing. The only way
 * to write one is a downgrade after a future mode is added, and a theme is not worth
 * crashing over.
 */
@Singleton
class DefaultThemeService @Inject constructor(
    @ApplicationContext context: Context,
) : ThemeService {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readStoredMode())
    override val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    override fun setMode(mode: ThemeMode) {
        if (_mode.value == mode) return

        // Published first, committed second: the repaint is what the user is waiting on,
        // and `apply` writes to disk off the caller's thread either way.
        _mode.value = mode
        preferences.edit().putString(KEY_MODE, mode.name).apply()
    }

    private fun readStoredMode(): ThemeMode {
        val stored = preferences.getString(KEY_MODE, null) ?: return ThemeMode.Default
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.Default
    }

    private companion object {
        const val PREFERENCES_NAME = "securevault.appearance"
        const val KEY_MODE = "theme_mode"
    }
}
