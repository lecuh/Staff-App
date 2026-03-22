package com.example.myapplication.data.model

data class MenuItem(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String?,
    val categoryName: String,
    val imageUrl: String? = null,
    val available: Boolean = true
)

data class PagedData(
    val content: List<MenuItem>,
    val totalPages: Int,
    val totalElements: Int,
    val size: Int,
    val number: Int
)

data class MenuResponse(
    val statusCode: Int,
    val message: String,
    val data: PagedData
)
