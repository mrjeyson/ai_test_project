package com.example.test_ai_project.core.network.api

import com.example.test_ai_project.core.network.dto.ItemDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ItemApi {

    @GET("posts")
    suspend fun getItems(): List<ItemDto>

    @GET("posts/{id}")
    suspend fun getItem(@Path("id") id: Long): ItemDto
}
