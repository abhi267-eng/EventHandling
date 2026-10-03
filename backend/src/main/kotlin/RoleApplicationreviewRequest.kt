package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class RoleApplicationReviewRequest(
    val applicationId: Long,
    val status: String,
    val comments: String? = null
)