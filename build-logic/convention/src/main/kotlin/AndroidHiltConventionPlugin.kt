import com.example.test_ai_project.buildlogic.libs
import com.example.test_ai_project.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Opt-in Hilt wiring. Applied per module rather than folded into the library convention,
 * because `:core:ui` needs no DI and `:core:domain` deliberately uses `hilt-core`
 * (javax.inject annotations only, no Dagger runtime and no code generation).
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies.apply {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-android-compiler"))
        }
    }
}
