package com.eventhandling

import java.sql.Connection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.random.Random

class EmailVerificationService(
    private val connection: Connection
) {

    fun generateAndStoreCode(userId: Long): String {

        val code = Random.nextInt(100000, 1000000).toString()

        val expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)

        val sql = """
            INSERT INTO email_verification_codes (
                user_id,
                code,
                expires_at
            )
            VALUES (?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, code)
            statement.setObject(3, expiresAt)

            statement.executeUpdate()
        }

        return code
    }

    fun verifyCode(userId: Long, code: String): Boolean {

        val sql = """
            SELECT verification_id
            FROM email_verification_codes
            WHERE user_id = ?
              AND code = ?
              AND expires_at > CURRENT_TIMESTAMP
              AND verified_at IS NULL
            ORDER BY created_at DESC
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, userId)
            statement.setString(2, code)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    return false
                }

                val verificationId = result.getLong("verification_id")

                val verifySql = """
                    UPDATE email_verification_codes
                    SET verified_at = CURRENT_TIMESTAMP
                    WHERE verification_id = ?
                """.trimIndent()

                connection.prepareStatement(verifySql).use { verifyStatement ->
                    verifyStatement.setLong(1, verificationId)
                    verifyStatement.executeUpdate()
                }

                val userSql = """
                    UPDATE users
                    SET email_verified = true,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ?
                """.trimIndent()

                connection.prepareStatement(userSql).use { userStatement ->
                    userStatement.setLong(1, userId)
                    userStatement.executeUpdate()
                }

                return true
            }
        }
    }
}