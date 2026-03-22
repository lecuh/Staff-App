package com.example.myapplication.data.model

data class Reservation(
    val id: Int,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val partySize: Int,
    val reservationTime: String,
    val status: String,
    val note: String?,
    val depositRequired: Boolean,
    val depositAmount: Double,
    val depositPaid: Boolean,
    val gracePeriodMinutes: Int,
    val arrivalTime: String?,
    val cancelledAt: String?,
    val cancellationReason: String?,
    val tableNumbers: List<String>,
    val billId: Int?,
    val createdAt: String,
    val updatedAt: String,
    val canCheckIn: Boolean,
    val canCancel: Boolean,
    val canMarkNoShow: Boolean
)
