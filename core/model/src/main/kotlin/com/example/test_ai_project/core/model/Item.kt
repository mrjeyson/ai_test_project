package com.example.test_ai_project.core.model

/**
 * A domain entity — the shape the rest of the app reasons about.
 *
 * Intentionally free of Room annotations and `@Serializable`: those belong to
 * [com.example.test_ai_project.core.database] and
 * [com.example.test_ai_project.core.network] respectively, so a change to the
 * database schema or the API payload cannot ripple into the UI.
 */
data class Item(
    val id: Long,
    val name: String,
    val description: String,
)
