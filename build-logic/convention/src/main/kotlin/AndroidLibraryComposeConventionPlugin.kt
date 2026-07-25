import com.android.build.api.dsl.LibraryExtension
import com.example.test_ai_project.buildlogic.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** An Android library that renders Compose UI. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("testaiproject.android.library")

        extensions.configure<LibraryExtension> {
            configureCompose(this)
        }
    }
}
