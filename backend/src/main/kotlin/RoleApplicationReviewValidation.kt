package com.eventhandling

fun validateRoleApplicationReview(
    request: RoleApplicationReviewRequest
): String? {

    if (request.applicationId <= 0) {
        return "Invalid application ID"
    }

    if (
        request.status != "APPROVED" &&
        request.status != "REJECTED"
    ) {
        return "Review status must be APPROVED or REJECTED"
    }

    if (
        request.status == "REJECTED" &&
        request.comments.isNullOrBlank()
    ) {
        return "Comments are required when rejecting an application"
    }

    return null
}