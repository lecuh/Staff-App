package com.example.myapplication.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    val tableService: TableService by lazy {
        retrofit.create(TableService::class.java)
    }

    val menuService: MenuService by lazy {
        retrofit.create(MenuService::class.java)
    }

    val orderService: OrderService by lazy {
        retrofit.create(OrderService::class.java)
    }

    val reservationService: ReservationService by lazy {
        retrofit.create(ReservationService::class.java)
    }
}
