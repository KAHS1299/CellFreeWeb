package com.example.cellfreeapp.data.models

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String
)

data class UpdateProfileRequest(
    val name: String?,
    val email: String?,
    val password: String?,
    val password_confirmation: String?
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val email_verified_at: String?,
    val created_at: String,
    val updated_at: String
)

data class AuthResponse(
    val status: String,
    val message: String,
    val data: AuthData?
)

data class AuthData(
    val user: User,
    val access_token: String,
    val token_type: String
)

data class ApiError(
    val status: String,
    val message: String,
    val errors: Map<String, List<String>>?
)