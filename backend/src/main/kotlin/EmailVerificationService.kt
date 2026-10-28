package com.eventhandling

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.time.OffsetDateTime
import java.time.ZoneOffset

class EmailVerificationService(
    private val connection: Connection,
    private val emailSender: EmailSender = EmailSenders.default
) {

    companion object {
        const val CODE_VALID_MINUTES = 10L
        const val RESEND_COOLDOWN_SECONDS = 60L
        const val MAX_CODES_PER_DAY = 5
        const val MAX_FAILED_ATTEMPTS = 5

        private val random = SecureRandom()
        private val logger = LoggerFactory.getLogger(EmailVerificationService::class.java)
    }

    /*
     * Creates a new code and invalidates any older unused codes.
     */
    fun generateAndStoreCode(userId: Long): String {

        val code = (random.nextInt(900000) + 100000).toString()

        val expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(CODE_VALID_MINUTES)

        val invalidateSql = """
            UPDATE email_verification_codes
            SET expires_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
              AND verified_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP
        """.trimIndent()

        connection.prepareStatement(invalidateSql).use { statement ->
            statement.setLong(1, userId)
            statement.executeUpdate()
        }

        val insertSql = """
            INSERT INTO email_verification_codes (
                user_id,
                code,
                expires_at
            )
            VALUES (?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(insertSql).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, code)
            statement.setObject(3, expiresAt)

            statement.executeUpdate()
        }

        return code
    }

    /*
     * Creates a code and emails it. A failed email never breaks
     * signup, because the user can request a new code.
     */
    fun sendCode(userId: Long, email: String) {

        val code = generateAndStoreCode(userId)

        try {
            emailSender.send(
                to = email,
                subject = "Your EventHandling verification code",
                body = "Your verification code is $code.\n\n" +
                    "It expires in $CODE_VALID_MINUTES minutes. " +
                    "If you did not sign up for EventHandling, you can ignore this email."
            )
        } catch (exception: Exception) {
            logger.error("Could not send verification email to $email", exception)
        }
    }

    /*
     * Email of an ACTIVE user who has not verified yet, otherwise null.
     */
    fun getUnverifiedEmail(userId: Long): String? {

        val sql = """
            SELECT email
            FROM users
            WHERE user_id = ?
              AND email_verified = false
              AND account_status = 'ACTIVE'
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)

            statement.executeQuery().use { result ->
                return if (result.next()) result.getString("email") else null
            }
        }
    }

    fun secondsUntilResendAllowed(userId: Long): Long {

        val sql = """
            SELECT EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - MAX(created_at)))
            FROM email_verification_codes
            WHERE user_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)

            statement.executeQuery().use { result ->
                result.next()

                val elapsedSeconds = result.getDouble(1)

                if (result.wasNull()) {
                    return 0
                }

                val remaining = RESEND_COOLDOWN_SECONDS - elapsedSeconds.toLong()
                return if (remaining > 0) remaining else 0
            }
        }
    }

    fun codesSentLast24Hours(userId: Long): Int {

        val sql = """
            SELECT COUNT(*)
            FROM email_verification_codes
            WHERE user_id = ?
              AND created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)

            statement.executeQuery().use { result ->
                result.next()
                return result.getInt(1)
            }
        }
    }

    fun verifyCode(userId: Long, code: String): Boolean {

        val sql = """
            SELECT verification_id, code, failed_attempts
            FROM email_verification_codes
            WHERE user_id = ?
              AND verified_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP
            ORDER BY created_at DESC
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, userId)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    return false
                }

                val verificationId = result.getLong("verification_id")
                val storedCode = result.getString("code")
                val failedAttempts = result.getInt("failed_attempts")

                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    return false
                }

                val matches = MessageDigest.isEqual(
                    storedCode.toByteArray(),
                    code.trim().toByteArray()
                )

                if (!matches) {
                    recordFailedAttempt(verificationId)
                    return false
                }

                markVerified(verificationId, userId)

                return true
            }
        }
    }

    private fun recordFailedAttempt(verificationId: Long) {

        val sql = """
            UPDATE email_verification_codes
            SET failed_attempts = failed_attempts + 1
            WHERE verification_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, verificationId)
            statement.executeUpdate()
        }
    }

    private fun markVerified(verificationId: Long, userId: Long) {

        val verifySql = """
            UPDATE email_verification_codes
            SET verified_at = CURRENT_TIMESTAMP
            WHERE verification_id = ?
        """.trimIndent()

        connection.prepareStatement(verifySql).use { statement ->
            statement.setLong(1, verificationId)
            statement.executeUpdate()
        }

        val userSql = """
            UPDATE users
            SET email_verified = true,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
        """.trimIndent()

        connection.prepareStatement(userSql).use { statement ->
            statement.setLong(1, userId)
            statement.executeUpdate()
        }
    }
}