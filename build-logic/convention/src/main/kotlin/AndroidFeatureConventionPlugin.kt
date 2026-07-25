import com.example.test_ai_project.buildlogic.libs
import com.example.test_ai_project.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Everything a feature module needs: a Compose Android library, Hilt, type-safe
 * navigation, and the core layers a feature is allowed to see.
 *
 * Note which dependency is absent: `:core:data`. A feature talks to use cases in
 * `:core:domain` and must not reach the data layer, so the convention encodes that rule
 * rather than leaving it to reviewer discipline.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("testaiproject.android.library.compose")
            apply("testaiproject.android.hilt")
            // Type-safe navigation routes are @Serializable, so every feature needs this.
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        dependencies.apply {
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))

            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))
        }
    }
}
