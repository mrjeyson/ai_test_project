package com.example.test_ai_project.resource.theme

import kotlinx.coroutines.flow.StateFlow

/** Which of the two palettes [AppTheme] paints. */
enum class ThemeMode {
    Light,
    Dark,
    ;

    companion object {
        /**
         * What an install with no stored choice gets.
         *
         * Light, not the system setting: the designs are drawn light, so that is the app as
         * intended until the user says otherwise on the settings tab.
         */
        val Default: ThemeMode = Light
    }
}

/**
 * The stored theme choice, and the only way to change it.
 *
 * A contract in the design system rather than in a feature's `domain`, because no one
 * feature owns it: the settings tab writes it, `:app` reads it to build the theme, and
 * every screen in between is painted by the result. It sits beside [AppTheme] for the same
 * reason the colour scheme does.
 *
 * A [StateFlow] rather than a suspending read: the theme has to be known before the first
 * frame, and a screen that had to wait for it would flash the wrong palette on the way in.
 *
 * An interface with one implementation, unusually for this project — the implementation
 * needs a `Context`, and a ViewModel test that had to stand one up would be a Robolectric
 * test rather than a JVM one.
 */
interface ThemeService {

    val mode: StateFlow<ThemeMode>

    /** Persists [mode] and republishes it to everything currently painted. */
    fun setMode(mode: ThemeMode)
}
