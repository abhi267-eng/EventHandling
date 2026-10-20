package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class MyEventResponse(
    val event: Event,
    val reviewComments: String?
)