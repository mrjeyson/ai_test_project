import com.android.build.api.dsl.ApplicationExtension
import com.example.test_ai_project.buildlogic.configureCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** An application module whose UI is Compose. */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("testaiproject.android.application")

        extensions.configure<ApplicationExtension> {
            configureCompose(this)
        }
    }
}
