package com.eventhandling

import java.sql.Connection
import java.sql.ResultSet

class AdminService(
    private val connection: Connection
) {

    private val columns = """
        user_id, name, email, phone, profile_picture_url, college_id,
        role, designation, email_verified, account_status
    """.trimIndent()

    private fun ResultSet.toUser() = User(
        userId = getLong("user_id"),
        name = getString("name"),
        email = getString("email"),
        phone = getString("phone"),
        profilePictureUrl = getString("profile_picture_url"),
        collegeId = getLong("college_id").let { if (wasNull()) null else it },
        role = getString("role"),
        designation = getString("designation"),
        emailVerified = getBoolean("email_verified"),
        accountStatus = getString("account_status")
    )

    fun getUserById(userId: Long): User? {
        val sql = "SELECT $columns FROM users WHERE user_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.executeQuery().use { result ->
                return if (result.next()) result.toUser() else null
            }
        }
    }

    fun listUsers(
        collegeId: Long?,
        role: String?,
        status: String?,
        search: String?,
        limit: Int,
        offset: Int
    ): List<User> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (collegeId != null) { conditions += "college_id = ?"; params += collegeId }
        if (role != null) { conditions += "role = ?"; params += role }
        if (status != null) { conditions += "account_status = ?"; params += status }
        if (search != null) {
            conditions += "(LOWER(name) LIKE ? OR LOWER(email) LIKE ?)"
            val pattern = "%${search.lowercase()}%"
            params += pattern
            params += pattern
        }

        val where = if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")
        val sql = "SELECT $columns FROM users $where ORDER BY user_id LIMIT ? OFFSET ?"
        params += limit
        params += offset

        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, value -> statement.setObject(index + 1, value) }

            statement.executeQuery().use { result ->
                val users = mutableListOf<User>()
                while (result.next()) users += result.toUser()
                return users
            }
        }
    }

    fun setAccountStatus(userId: Long, status: String): Boolean {
        val sql = "UPDATE users SET account_status = ? WHERE user_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, status)
            statement.setLong(2, userId)
            return statement.executeUpdate() > 0
        }
    }

    fun softDeleteUser(userId: Long): Boolean {
        val sql = """
            UPDATE users
            SET account_status = 'DELETED',
                name = 'Deleted User',
                email = 'deleted_' || user_id || '@deleted.invalid',
                phone = NULL,
                profile_picture_url = NULL
            WHERE user_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            return statement.executeUpdate() > 0
        }
    }
}