plugins {
    id("testai.android.library")
    id("testai.android.hilt")
}

android {
    namespace = "com.example.test_ai_project.home.data"
}

dependencies {
    implementation(project(":feature:home:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // The fused location provider. Confined to this module by the same rule that confines
    // Retrofit here: acquiring a fix is data-layer work, and the map only renders the
    // cached result.
    implementation(libs.play.services.location)

    // NotificationCompat and NotificationManagerCompat, for the prayer alerts. The alarms
    // themselves need no dependency — AlarmManager is a platform service — but posting a
    // notification that behaves the same from API 24 to 37 does.
    implementation(libs.androidx.core.ktx)

    // The prayer alert copy lives with every other user-facing string.
    implementation(project(":core:resource"))
}
