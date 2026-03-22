package com.example.myapplication.data.model

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int = 1
)

data class OrderDetail(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val price: Double,
    val subtotal: Double,
    val itemStatus: String,
    val notes: String?
)

data class OrderDetailResponse(
    val statusCode: Int,
    val message: String,
    val data: OrderDetail,
    val responseAt: String
)
