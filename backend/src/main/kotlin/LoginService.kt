package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val message: String,
    val user: User,
    val token: String
)