// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// Every alias below is `apply false` — nothing is applied to the root project. They are
// declared here so the plugin JARs land on the root project's buildscript classpath, which
// subprojects inherit. The convention plugins in `build-logic` declare these same artifacts
// as `compileOnly` (they only need the DSL to compile against), so this block is what makes
// their `pluginManager.apply("com.android.library")` calls resolve at execution time.
//
// Removing an alias here will surface as "Plugin with id '...' not found" from inside a
// convention plugin, not from the module that requested it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
