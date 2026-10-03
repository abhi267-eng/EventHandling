package com.eventhandling

import java.sql.Connection

class AuthService(
    private val connection: Connection
) {

    fun login(request: LoginRequest): User? {

        val sql = """
            SELECT user_id,
                   name,
                   email,
                   password_hash,
                   phone,
                   profile_picture_url,
                   college_id,
                   role,
                   designation,
                   email_verified,
                   account_status
            FROM users
            WHERE email = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setString(1, request.email)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    return null
                }

                val passwordHash = result.getString("password_hash")

                if (!PasswordHasher.verify(request.password, passwordHash)) {
                    return null
                }

                if (!result.getBoolean("email_verified")) {
                    throw IllegalStateException("Email is not verified")
                }

                if (result.getString("account_status") != "ACTIVE") {
                    throw IllegalStateException("Account is not active")
                }

                return User(
                    userId = result.getLong("user_id"),
                    name = result.getString("name"),
                    email = result.getString("email"),
                    phone = result.getString("phone"),
                    profilePictureUrl = result.getString("profile_picture_url"),
                    collegeId = result.getLong("college_id").let {
                        if (result.wasNull()) null else it
                    },
                    role = result.getString("role"),
                    designation = result.getString("designation"),
                    emailVerified = result.getBoolean("email_verified"),
                    accountStatus = result.getString("account_status")
                )
            }
        }
    }
}