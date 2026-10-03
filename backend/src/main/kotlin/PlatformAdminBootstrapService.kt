package com.eventhandling

import java.sql.Connection

class PlatformAdminBootstrapService(
    private val connection: Connection
) {

    fun platformAdminExists(): Boolean {
        val sql = """
            SELECT EXISTS(
                SELECT 1
                FROM users
                WHERE role = 'PLATFORM_ADMIN'
            )
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { result ->
                result.next()
                return result.getBoolean(1)
            }
        }
    }

    fun createPlatformAdmin(
        name: String,
        email: String,
        password: String,
        phone: String?,
        profilePictureUrl: String?
    ): User {

        if (platformAdminExists()) {
            throw IllegalStateException("Platform Admin already exists")
        }

        val passwordHash = PasswordHasher.hash(password)

        val sql = """
            INSERT INTO users (
                name,
                email,
                password_hash,
                phone,
                profile_picture_url,
                role,
                email_verified,
                account_status
            )
            VALUES (
                ?, ?, ?, ?, ?,
                'PLATFORM_ADMIN',
                true,
                'ACTIVE'
            )
            RETURNING
                user_id,
                name,
                email,
                phone,
                profile_picture_url,
                college_id,
                role,
                designation,
                email_verified,
                account_status
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setString(1, name)
            statement.setString(2, email)
            statement.setString(3, passwordHash)
            statement.setString(4, phone)
            statement.setString(5, profilePictureUrl)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    throw IllegalStateException("Failed to create Platform Admin")
                }

                return User(
                    userId = result.getLong("user_id"),
                    name = result.getString("name"),
                    email = result.getString("email"),
                    phone = result.getString("phone"),
                    profilePictureUrl = result.getString("profile_picture_url"),
                    collegeId = result.getLong("college_id")
                        .takeIf { !result.wasNull() },
                    role = result.getString("role"),
                    designation = result.getString("designation"),
                    emailVerified = result.getBoolean("email_verified"),
                    accountStatus = result.getString("account_status")
                )
            }
        }
    }
}