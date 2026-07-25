import androidx.room.gradle.RoomExtension
import com.example.test_ai_project.buildlogic.libs
import com.example.test_ai_project.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Room, including schema export.
 *
 * The schema directory is resolved from `layout.projectDirectory` at configuration time so
 * the value is a plain path — nothing captures `Project` into a task action, which keeps
 * this compatible with the configuration cache.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("androidx.room")
        }

        extensions.configure<RoomExtension> {
            // Checked in, so migrations can be diffed against a known-good baseline.
            schemaDirectory("${layout.projectDirectory}/schemas")
        }

        dependencies.apply {
            add("api", libs.library("androidx-room-runtime"))
            add("api", libs.library("androidx-room-ktx"))
            add("ksp", libs.library("androidx-room-compiler"))
        }
    }
}
