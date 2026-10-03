package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class PatchEventRequest(
    val title: String? = null,
    val description: String? = null,
    val collegeId: Long? = null,
    val createdBy: Long? = null,
    val categoryId: Long? = null,
    val rules: String? = null,
    val startDatetime: String? = null,
    val endDatetime: String? = null,
    val venueType: String? = null,
    val venueName: String? = null,
    val venueAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val acceptVisitors: Boolean? = null,
    val posterUrl: String? = null
)