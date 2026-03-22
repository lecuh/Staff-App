package com.example.myapplication.data.api

import com.example.myapplication.data.model.Reservation
import retrofit2.Response
import retrofit2.http.*

interface ReservationService {
    @GET("api/reservations")
    suspend fun getReservations(
        @Header("Authorization") token: String,
        @Query("status") status: String
    ): Response<List<Reservation>>

    @PUT("api/reservations/{id}/confirm")
    suspend fun confirmReservation(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Reservation>

    @PUT("api/reservations/{id}/cancel")
    suspend fun cancelReservation(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Reservation>

    @PUT("api/reservations/{id}/check-in")
    suspend fun checkInReservation(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Reservation>

    @PUT("api/reservations/{id}/no-show")
    suspend fun markNoShow(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Query("reason") reason: String? = null
    ): Response<Reservation>
}
