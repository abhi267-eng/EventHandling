package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class CollegeLookup(
    val collegeId: Long,
    val collegeName: String,
    val collegeCode: String,
    val city: String?,
    val address: String?,
    val website: String?,
    val logoUrl: String?
)

@Serializable
data class DepartmentLookup(
    val departmentId: Long,
    val departmentName: String,
    val departmentCode: String
)

@Serializable
data class OrganizationLookup(
    val organizationId: Long,
    val organizationName: String,
    val organizationType: String
)
