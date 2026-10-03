package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class PlatformAdminBootstrapRequest(
    val setupSecret: String,
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val profilePictureUrl: String? = null
)