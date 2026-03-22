package com.example.myapplication.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val statusCode: Int,
    val message: String,
    val data: LoginData?,
    val responseTime: String
)

data class LoginData(
    val token: String,
    val refreshToken: String,
    val type: String,
    val user: UserInfo
)

data class UserInfo(
    val id: Int,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
    val active: Boolean
)
