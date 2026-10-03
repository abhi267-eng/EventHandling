package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val profilePictureUrl: String?,
    val collegeId: Long?,
    val role: String,
    val designation: String?,
    val emailVerified: Boolean,
    val accountStatus: String
)