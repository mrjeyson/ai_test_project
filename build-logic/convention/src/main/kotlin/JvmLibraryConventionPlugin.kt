import com.example.test_ai_project.buildlogic.configureKotlinJvm
import com.example.test_ai_project.buildlogic.configureUnitTestDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A pure-Kotlin module with no Android dependency — used by the innermost clean
 * architecture layers, so their business rules stay testable on the plain JVM.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        configureKotlinJvm()
        configureUnitTestDependencies()
    }
}
