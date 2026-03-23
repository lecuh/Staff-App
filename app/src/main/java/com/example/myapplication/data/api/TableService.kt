package com.example.myapplication.data.api

import com.example.myapplication.data.model.BillResponse
import com.example.myapplication.data.model.CreateBillRequest
import com.example.myapplication.data.model.RestaurantTable
import com.example.myapplication.data.model.TableResponse
import retrofit2.Response
import retrofit2.http.*

interface TableService {
    @GET("api/tables")
    suspend fun getTables(@Header("Authorization") token: String): Response<TableResponse>

    @PATCH("api/tables/{id}/status")
    suspend fun updateTableStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Query("status") status: String
    ): Response<TableStatusResponse>

    @POST("api/bills")
    suspend fun createBill(
        @Header("Authorization") token: String,
        @Body request: CreateBillRequest
    ): Response<BillResponse>
}

data class TableStatusResponse(
    val statusCode: Int,
    val message: String,
    val data: RestaurantTable
)
