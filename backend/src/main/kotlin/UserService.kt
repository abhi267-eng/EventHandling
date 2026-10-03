package com.eventhandling

import java.sql.Connection

class UserService(
    private val connection: Connection
) {

    fun emailExists(email: String): Boolean {
        val sql = """
            SELECT EXISTS(
                SELECT 1
                FROM users
                WHERE email = ?
            )
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, email)

            statement.executeQuery().use { result ->
                result.next()
                return result.getBoolean(1)
            }
        }
    }

fun createNormalUser(request: SignupRequest): User {
    val passwordHash = PasswordHasher.hash(request.password)

    val sql = """
        INSERT INTO users (
            name,
            email,
            password_hash,
            phone,
            profile_picture_url,
            college_id,
            role,
            email_verified,
            account_status
        )
        VALUES (?, ?, ?, ?, ?, ?, 'NORMAL_USER', false, 'ACTIVE')
        RETURNING user_id, name, email, phone,
                  profile_picture_url, college_id,
                  role, designation,
                  email_verified, account_status
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->

        statement.setString(1, request.name)
        statement.setString(2, request.email)
        statement.setString(3, passwordHash)
        statement.setString(4, request.phone)
        statement.setString(5, request.profilePictureUrl)

        if (request.collegeId != null) {
            statement.setLong(6, request.collegeId)
        } else {
            statement.setNull(6, java.sql.Types.BIGINT)
        }

        statement.executeQuery().use { result ->
            result.next()

            val user = User(
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

            val emailVerificationService = EmailVerificationService(connection)
            val code = emailVerificationService.generateAndStoreCode(user.userId)

            println("OTP for ${user.email}: $code")

            return user
        }
    }
}
}