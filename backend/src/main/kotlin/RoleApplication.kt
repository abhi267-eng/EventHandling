package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class RoleApplication(
    val applicationId: Long,
    val role: String,

    val name: String,
    val email: String,
    val phone: String?,
    val profilePictureUrl: String?,

    val collegeId: Long?,
    val collegeIdNumber: String?,
    val collegeIdPhotoUrl: String?,

    val designation: String?,

    val requestedDepartmentId: Long?,
    val requestedOrganizationId: Long?,

    val status: String
)