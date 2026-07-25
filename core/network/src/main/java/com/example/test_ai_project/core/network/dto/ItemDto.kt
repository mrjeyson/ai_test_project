package com.example.test_ai_project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format. Field names follow the API, not the app — `@SerialName` absorbs
 * the mismatch so a rename on the backend is a one-line change in this file.
 */
@Serializable
data class ItemDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val name: String,
    @SerialName("body") val description: String = "",
)
