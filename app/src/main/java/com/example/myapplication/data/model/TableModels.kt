package com.example.myapplication.data.model

data class TableResponse(
    val statusCode: Int,
    val message: String,
    val data: List<RestaurantTable>
)

data class RestaurantTable(
    val id: Int,
    val tableNumber: String,
    val currentBill: Any?, // Can be more specific if needed later
    val capacity: Int,
    val status: String, // AVAILABLE, OCCUPIED, RESERVED
    val location: String,
    val qrCode: String,
    val createdAt: String,
    val updatedAt: String
)
