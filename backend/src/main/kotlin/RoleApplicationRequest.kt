package com.eventhandling

import kotlinx.serialization.Serializable

@Serializable
data class RoleApplicationRequest(
    val role: String,

    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val profilePictureUrl: String? = null,

    val collegeId: Long? = null,
    val collegeIdNumber: String? = null,
    val collegeIdPhotoUrl: String? = null,

    val designation: String? = null,

    // Student Organizer / Faculty Coordinator
    val departmentId: Long? = null,

    // Student Organizer / Event Coordinator
    val organizationId: Long? = null,

    // Used when applying as College Admin
    val proposedCollegeName: String? = null,
    val proposedCollegeCode: String? = null,
    val proposedEmailDomain: String? = null,
    val proposedAddress: String? = null,
    val proposedCity: String? = null,
    val proposedLatitude: Double? = null,
    val proposedLongitude: Double? = null,
    val proposedWebsite: String? = null,
    val proposedLogoUrl: String? = null
)