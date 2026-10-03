package com.eventhandling

fun validateRoleApplication(
    request: RoleApplicationRequest
): String? {

    val allowedRoles = setOf(
        "STUDENT_ORGANIZER",
        "FACULTY_COORDINATOR",
        "EVENT_COORDINATOR",
        "COLLEGE_ADMIN"
    )

    if (request.role !in allowedRoles) {
        return "Invalid application role"
    }

    if (request.name.isBlank()) {
        return "Name is required"
    }

    if (request.email.isBlank()) {
        return "Email is required"
    }

    if (!request.email.contains("@")) {
        return "Invalid email address"
    }

    if (request.password.length < 8) {
        return "Password must be at least 8 characters"
    }

    /*
     * All elevated roles require college verification.
     * College Admin is the exception only when proposing
     * a completely new college.
     */
    if (request.role != "COLLEGE_ADMIN") {

        if (request.collegeId == null) {
            return "College is required"
        }

        if (request.collegeIdNumber.isNullOrBlank()) {
            return "College ID number is required"
        }

        if (request.collegeIdPhotoUrl.isNullOrBlank()) {
            return "College ID photo is required"
        }
    }

    when (request.role) {

        "STUDENT_ORGANIZER" -> {

            if (request.collegeId == null) {
                return "College is required"
            }

            if (
                request.departmentId == null &&
                request.organizationId == null
            ) {
                return "At least one department or organization is required"
            }
        }

        "FACULTY_COORDINATOR" -> {

            if (request.collegeId == null) {
                return "College is required"
            }

            if (request.departmentId == null) {
                return "Department is required"
            }

            if (request.designation.isNullOrBlank()) {
                return "Designation is required"
            }
        }

        "EVENT_COORDINATOR" -> {

            if (request.collegeId == null) {
                return "College is required"
            }

            if (request.organizationId == null) {
                return "Organization is required"
            }

            if (request.designation.isNullOrBlank()) {
                return "Designation is required"
            }
        }

        "COLLEGE_ADMIN" -> {

            if (request.designation.isNullOrBlank()) {
                return "Designation is required"
            }

            if (request.collegeIdNumber.isNullOrBlank()) {
                return "College ID number is required"
            }

            if (request.collegeIdPhotoUrl.isNullOrBlank()) {
                return "College ID photo is required"
            }

            /*
             * Existing college
             */
            if (request.collegeId != null) {

                if (
                    request.proposedCollegeName != null ||
                    request.proposedCollegeCode != null ||
                    request.proposedEmailDomain != null ||
                    request.proposedAddress != null ||
                    request.proposedCity != null ||
                    request.proposedLatitude != null ||
                    request.proposedLongitude != null ||
                    request.proposedWebsite != null ||
                    request.proposedLogoUrl != null
                ) {
                    return "Do not provide new college details when an existing college is selected"
                }
            }

            /*
             * New college
             */
            else {

                if (request.proposedCollegeName.isNullOrBlank()) {
                    return "New college name is required"
                }

                if (request.proposedCollegeCode.isNullOrBlank()) {
                    return "New college code is required"
                }

                if (request.proposedEmailDomain.isNullOrBlank()) {
                    return "Official email domain is required"
                }

                if (request.proposedAddress.isNullOrBlank()) {
                    return "College address is required"
                }

                if (request.proposedCity.isNullOrBlank()) {
                    return "College city is required"
                }

                if (
                    request.proposedLatitude == null ||
                    request.proposedLongitude == null
                ) {
                    return "College location is required"
                }
            }
        }
    }

    return null
}