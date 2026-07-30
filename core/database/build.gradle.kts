plugins {
    id("testai.android.library")
    id("testai.android.hilt")
    id("testai.android.room")
}

android {
    namespace = "com.example.test_ai_project.database"
}

// Room artifacts, the KSP processor, and the checked-in schema directory all come from
// the `testai.android.room` convention.
