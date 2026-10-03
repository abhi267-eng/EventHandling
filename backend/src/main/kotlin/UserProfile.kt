package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val profilePictureUrl: String?,
    val collegeId: Long?,
    val collegeName: String?,
    val role: String,
    val designation: String?,
    val emailVerified: Boolean,
    val accountStatus: String
)

@Serializable
data class UserDepartment(
    val departmentId: Long,
    val departmentName: String,
    val departmentCode: String
)

@Serializable
data class UserOrganization(
    val organizationId: Long,
    val organizationName: String,
    val organizationType: String
)