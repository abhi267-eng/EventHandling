package com.eventhandling

import java.sql.Connection

class UserProfileService(
    private val connection: Connection
) {

    fun getUserProfile(userId: Long): UserProfile? {

        val sql = """
            SELECT
                u.user_id,
                u.name,
                u.email,
                u.phone,
                u.profile_picture_url,
                u.college_id,
                c.college_name,
                u.role,
                u.designation,
                u.email_verified,
                u.account_status
            FROM users u
            LEFT JOIN colleges c
                ON u.college_id = c.college_id
            WHERE u.user_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, userId)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    return null
                }

                val collegeId =
                    result.getLong("college_id")
                        .let { if (result.wasNull()) null else it }

                return UserProfile(
                    userId = result.getLong("user_id"),
                    name = result.getString("name"),
                    email = result.getString("email"),
                    phone = result.getString("phone"),
                    profilePictureUrl =
                        result.getString("profile_picture_url"),
                    collegeId = collegeId,
                    collegeName = result.getString("college_name"),
                    role = result.getString("role"),
                    designation = result.getString("designation"),
                    emailVerified = result.getBoolean("email_verified"),
                    accountStatus = result.getString("account_status")
                )
            }
        }
    }

    fun getDepartments(userId: Long): List<UserDepartment> {

        val sql = """
            SELECT
                d.department_id,
                d.department_name,
                d.department_code
            FROM user_departments ud
            JOIN departments d
                ON ud.department_id = d.department_id
            WHERE ud.user_id = ?
            ORDER BY d.department_name
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, userId)

            statement.executeQuery().use { result ->

                val departments = mutableListOf<UserDepartment>()

                while (result.next()) {
                    departments.add(
                        UserDepartment(
                            departmentId =
                                result.getLong("department_id"),
                            departmentName =
                                result.getString("department_name"),
                            departmentCode =
                                result.getString("department_code")
                        )
                    )
                }

                return departments
            }
        }
    }

    fun getOrganizations(userId: Long): List<UserOrganization> {

        val sql = """
            SELECT
                o.organization_id,
                o.organization_name,
                o.organization_type
            FROM user_organizations uo
            JOIN organizations o
                ON uo.organization_id = o.organization_id
            WHERE uo.user_id = ?
            ORDER BY o.organization_name
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, userId)

            statement.executeQuery().use { result ->

                val organizations = mutableListOf<UserOrganization>()

                while (result.next()) {
                    organizations.add(
                        UserOrganization(
                            organizationId =
                                result.getLong("organization_id"),
                            organizationName =
                                result.getString("organization_name"),
                            organizationType =
                                result.getString("organization_type")
                        )
                    )
                }

                return organizations
            }
        }
    }
}