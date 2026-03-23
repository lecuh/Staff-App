package com.example.myapplication.data.model

data class TableResponse(
    val statusCode: Int,
    val message: String,
    val data: List<RestaurantTable>
)

data class RestaurantTable(
    val id: Int,
    val tableNumber: String,
    val currentBill: Any?,
    val capacity: Int,
    val status: String,
    val location: String,
    val qrCode: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreateBillRequest(
    val tableIds: List<Int>,
    val partySize: Int,
    val reservationId: Int? = null
)

data class BillData(
    val id: Int,
    val totalPrice: Double,
    val finalPrice: Double,
    val status: String
)

data class BillResponse(
    val statusCode: Int,
    val message: String,
    val data: BillData
)
