package com.example.myapplication.data.api

import com.example.myapplication.data.model.OrderDetail
import com.example.myapplication.data.model.OrderDetailResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface OrderService {
    @PATCH("api/order-details/{id}/status")
    @Headers("Content-Type: application/json")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: RequestBody
    ): Response<OrderDetailResponse>

    @GET("api/order-details/statusList")
    suspend fun getItemsByStatus(
        @Header("Authorization") token: String,
        @Query("status") status: String
    ): Response<OrderListResponse>
}

data class OrderListResponse(
    val statusCode: Int,
    val message: String,
    val data: List<OrderDetail>,
    val responseAt: String
)
