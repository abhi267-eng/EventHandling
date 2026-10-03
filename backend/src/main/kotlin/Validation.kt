package com.eventhandling

fun validateSignupRequest(request: SignupRequest): String? {

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

    if (request.collegeId != null) {

        if (request.collegeIdNumber.isNullOrBlank()) {
            return "College ID number is required when college is selected"
        }

        if (request.collegeIdPhotoUrl.isNullOrBlank()) {
            return "College ID photo is required when college is selected"
        }
    }

    return null
}