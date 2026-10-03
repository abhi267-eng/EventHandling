package com.eventhandling

import io.ktor.server.auth.jwt.*

fun JWTPrincipal.hasRole(role: String): Boolean {
    return payload.getClaim("role").asString() == role
}

fun JWTPrincipal.hasAnyRole(vararg roles: String): Boolean {
    val userRole = payload.getClaim("role").asString()
    return userRole in roles
}
