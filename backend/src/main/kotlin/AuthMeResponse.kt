package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class AuthMeResponse(
    val profile: UserProfile,
    val departments: List<UserDepartment>,
    val organizations: List<UserOrganization>
)