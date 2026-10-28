package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class ResendOtpRequest(
    val userId: Long
)