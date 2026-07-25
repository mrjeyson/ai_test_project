plugins {
    id("testaiproject.android.library")
    id("testaiproject.android.hilt")
    id("testaiproject.android.room")
}

android {
    namespace = "com.example.test_ai_project.core.database"
}

// Room artifacts, the KSP processor, and the checked-in schema directory all come from
// the `testaiproject.android.room` convention.
