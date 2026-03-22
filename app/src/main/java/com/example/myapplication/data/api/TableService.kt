package com.example.myapplication.data.api

import com.example.myapplication.data.model.RestaurantTable
import com.example.myapplication.data.model.TableResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface TableService {
    @GET("api/tables")
    suspend fun getTables(@Header("Authorization") token: String): Response<TableResponse>

    @PATCH("api/tables/{id}/status")
    suspend fun updateTableStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Query("status") status: String
    ): Response<TableStatusResponse>
}

data class TableStatusResponse(
    val statusCode: Int,
    val message: String,
    val data: RestaurantTable
)
