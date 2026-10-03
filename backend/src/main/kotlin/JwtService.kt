package com.eventhandling

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtService {

    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    private var expirationTime: Long = 0

    fun configure(
        secret: String,
        issuer: String,
        audience: String,
        expirationTime: Long
    ) {
        this.secret = secret
        this.issuer = issuer
        this.audience = audience
        this.expirationTime = expirationTime
    }

    fun generateToken(user: User): String {

        val algorithm =
            Algorithm.HMAC256(secret)

        return JWT.create()
            .withSubject(user.userId.toString())
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("email", user.email)
            .withClaim("role", user.role)
            .withExpiresAt(
                Date(
                    System.currentTimeMillis() +
                            expirationTime
                )
            )
            .sign(algorithm)
    }
}