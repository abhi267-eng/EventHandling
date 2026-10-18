package com.eventhandling

import io.ktor.server.auth.jwt.*

fun JWTPrincipal.hasRole(role: String): Boolean {
    return payload.getClaim("role").asString() == role
}

fun JWTPrincipal.hasAnyRole(vararg roles: String): Boolean {
    val userRole = payload.getClaim("role").asString()
    return userRole in roles
}


fun canManageEvent(
    principal: JWTPrincipal,
    userId: Long,
    event: Event,
    userService: UserService
): Boolean {
    if (principal.hasRole("PLATFORM_ADMIN")) return true

    if (event.createdBy == userId) return true

    if (principal.hasRole("COLLEGE_ADMIN") &&
        userService.getCollegeId(userId) == event.collegeId
    ) {
        return true
    }

    return false
}