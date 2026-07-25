import com.android.build.api.dsl.ApplicationExtension
import com.example.test_ai_project.buildlogic.configureAndroidCommon
import com.example.test_ai_project.buildlogic.configureUnitTestDependencies
import com.example.test_ai_project.buildlogic.intVersion
import com.example.test_ai_project.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The application module. Shares SDK/Java configuration with libraries and adds the one
 * setting only an application has: `targetSdk`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = libs.intVersion("targetSdk")
        }
        configureUnitTestDependencies()
    }
}
