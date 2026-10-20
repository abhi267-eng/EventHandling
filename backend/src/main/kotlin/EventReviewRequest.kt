package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class EventReviewRequest(
    val status: String,
    val comments: String? = null
)