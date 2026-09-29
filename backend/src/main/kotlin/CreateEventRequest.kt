package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String?,
    val collegeId: Long,
    val createdBy: Long?,
    val categoryId: Long?,
    val rules: String?,
    val startDatetime: String,
    val endDatetime: String,
    val venueType: String,
    val venueName: String?,
    val venueAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val acceptVisitors: Boolean,
    val posterUrl: String?
)