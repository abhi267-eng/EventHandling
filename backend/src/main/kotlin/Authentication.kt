package com.eventhandling

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {

    val secret =
        environment.config
            .property("jwt.secret")
            .getString()

    val issuer =
        environment.config
            .property("jwt.issuer")
            .getString()

    val audience =
        environment.config
            .property("jwt.audience")
            .getString()

    val expirationTime =
        environment.config
            .property("jwt.expiration")
            .getString()
            .toLong()

    JwtService.configure(
        secret = secret,
        issuer = issuer,
        audience = audience,
        expirationTime = expirationTime
    )

    install(Authentication) {

        jwt("auth-jwt") {

            verifier(
                JWT
                    .require(
                        Algorithm.HMAC256(secret)
                    )
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )

            validate { credential ->

                val userId =
                    credential.payload.subject

                if (userId != null) {
                    JWTPrincipal(
                        credential.payload
                    )
                } else {
                    null
                }
            }

            challenge { _, _ ->

                call.respondText(
                    "Invalid or missing authentication token",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }
    }
}