package com.example.myapplication.data.api

import com.example.myapplication.data.model.MenuResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MenuService {
    @GET("api/items")
    suspend fun getMenuItems(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("sortBy") sortBy: String = "name",
        @Query("sortDirection") sortDirection: String = "ASC"
    ): Response<MenuResponse>
}
