package com.eventhandling

data class RoleApplicationDetails(
    val applicationId: Long,
    val role: String,
    val name: String,
    val email: String,
    val phone: String?,
    val passwordHash: String?,
    val profilePictureUrl: String?,

    val collegeId: Long?,
    val collegeIdNumber: String?,
    val collegeIdPhotoUrl: String?,

    val designation: String?,

    val requestedDepartmentId: Long?,
    val requestedOrganizationId: Long?,

    val proposedCollegeName: String?,
    val proposedCollegeCode: String?,
    val proposedEmailDomain: String?,
    val proposedAddress: String?,
    val proposedCity: String?,
    val proposedLatitude: Double?,
    val proposedLongitude: Double?,
    val proposedWebsite: String?,
    val proposedLogoUrl: String?,

    val status: String
)