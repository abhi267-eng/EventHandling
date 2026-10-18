package com.eventhandling

import io.ktor.server.auth.jwt.*

fun JWTPrincipal.hasRole(role: String): Boolean {
    return payload.getClaim("role").asString() == role
}

fun JWTPrincipal.hasAnyRole(vararg roles: String): Boolean {
    val userRole = payload.getClaim("role").asString()
    return userRole in roles
}
private val roleRank = mapOf(
    "PLATFORM_ADMIN" to 5,
    "COLLEGE_ADMIN" to 4,
    "FACULTY_COORDINATOR" to 3,
    "EVENT_COORDINATOR" to 3,
    "STUDENT_ORGANIZER" to 2,
    "NORMAL_USER" to 1
)

fun canManage(actor: User, target: User): Boolean {
    if (actor.userId == target.userId) return false

    val actorRank = roleRank[actor.role] ?: return false
    val targetRank = roleRank[target.role] ?: return false
    if (actorRank <= targetRank) return false

    return when (actor.role) {
        "PLATFORM_ADMIN" -> true
        "COLLEGE_ADMIN" -> actor.collegeId != null && actor.collegeId == target.collegeId
        else -> false
    }
}