import com.android.build.api.dsl.LibraryExtension
import com.example.test_ai_project.buildlogic.configureAndroidCommon
import com.example.test_ai_project.buildlogic.configureUnitTestDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The baseline Android library module.
 *
 * Deliberately does NOT touch `buildFeatures.buildConfig` — `:core:network` opts into it,
 * and forcing a default here would silently break that module.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
        }
        configureUnitTestDependencies()
    }
}
