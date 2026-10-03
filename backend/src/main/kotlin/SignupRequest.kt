package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val profilePictureUrl: String? = null,
    val collegeId: Long? = null,
    val departmentId: Long? = null,
    val collegeIdNumber: String? = null,
    val collegeIdPhotoUrl: String? = null
)