package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequest(
    val userId: Long,
    val code: String
)