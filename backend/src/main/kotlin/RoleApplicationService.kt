package com.eventhandling

import java.sql.Connection

class RoleApplicationService(
    private val connection: Connection
) {

    fun pendingApplicationExists(
        email: String,
        role: String
    ): Boolean {

        val sql = """
            SELECT application_id
            FROM role_applications
            WHERE LOWER(email) = LOWER(?)
              AND role = ?
              AND status = 'PENDING'
            LIMIT 1
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setString(1, email)
            statement.setString(2, role)

            statement.executeQuery().use { result ->
                return result.next()
            }
        }
    }

    fun createApplication(
        request: RoleApplicationRequest
    ): RoleApplication {

        val passwordHash =
            PasswordHasher.hash(request.password)

        val sql = """
            INSERT INTO role_applications (
                role,
                name,
                email,
                phone,
                password_hash,
                profile_picture_url,
                college_id,
                college_id_number,
                college_id_photo_url,
                designation,
                requested_department_id,
                requested_organization_id,

                proposed_college_name,
                proposed_college_code,
                proposed_email_domain,
                proposed_address,
                proposed_city,
                proposed_latitude,
                proposed_longitude,
                proposed_website,
                proposed_logo_url,

                status
            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?,
                'PENDING'
            )
            RETURNING
                application_id,
                role,
                name,
                email,
                phone,
                profile_picture_url,
                college_id,
                college_id_number,
                college_id_photo_url,
                designation,
                requested_department_id,
                requested_organization_id,
                status
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setString(1, request.role)
            statement.setString(2, request.name)
            statement.setString(3, request.email)
            statement.setString(4, request.phone)
            statement.setString(5, passwordHash)
            statement.setString(6, request.profilePictureUrl)

            if (request.collegeId != null) {
                statement.setLong(7, request.collegeId)
            } else {
                statement.setNull(7, java.sql.Types.BIGINT)
            }

            statement.setString(8, request.collegeIdNumber)
            statement.setString(9, request.collegeIdPhotoUrl)
            statement.setString(10, request.designation)

            if (request.departmentId != null) {
                statement.setLong(11, request.departmentId)
            } else {
                statement.setNull(11, java.sql.Types.BIGINT)
            }

            if (request.organizationId != null) {
                statement.setLong(12, request.organizationId)
            } else {
                statement.setNull(12, java.sql.Types.BIGINT)
            }

            statement.setString(13, request.proposedCollegeName)
            statement.setString(14, request.proposedCollegeCode)
            statement.setString(15, request.proposedEmailDomain)
            statement.setString(16, request.proposedAddress)
            statement.setString(17, request.proposedCity)

            if (request.proposedLatitude != null) {
                statement.setDouble(18, request.proposedLatitude)
            } else {
                statement.setNull(18, java.sql.Types.DOUBLE)
            }

            if (request.proposedLongitude != null) {
                statement.setDouble(19, request.proposedLongitude)
            } else {
                statement.setNull(19, java.sql.Types.DOUBLE)
            }

            statement.setString(20, request.proposedWebsite)
            statement.setString(21, request.proposedLogoUrl)

            statement.executeQuery().use { result ->

                result.next()

                return RoleApplication(
                    applicationId =
                        result.getLong("application_id"),

                    role =
                        result.getString("role"),

                    name =
                        result.getString("name"),

                    email =
                        result.getString("email"),

                    phone =
                        result.getString("phone"),

                    profilePictureUrl =
                        result.getString("profile_picture_url"),

                    collegeId =
                        result.getLong("college_id").let {
                            if (result.wasNull()) null else it
                        },

                    collegeIdNumber =
                        result.getString("college_id_number"),

                    collegeIdPhotoUrl =
                        result.getString("college_id_photo_url"),

                    designation =
                        result.getString("designation"),

                    requestedDepartmentId =
                        result.getLong(
                            "requested_department_id"
                        ).let {
                            if (result.wasNull()) null else it
                        },

                    requestedOrganizationId =
                        result.getLong(
                            "requested_organization_id"
                        ).let {
                            if (result.wasNull()) null else it
                        },

                    status =
                        result.getString("status")
                )
            }
        }
    }

    fun getApplicationRole(
        applicationId: Long
    ): String? {

        val sql = """
            SELECT role
            FROM role_applications
            WHERE application_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, applicationId)

            statement.executeQuery().use { result ->

                return if (result.next()) {
                    result.getString("role")
                } else {
                    null
                }
            }
        }
    }

    fun applicationExists(
        applicationId: Long
    ): Boolean {

        val sql = """
            SELECT application_id
            FROM role_applications
            WHERE application_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, applicationId)

            statement.executeQuery().use { result ->
                return result.next()
            }
        }
    }

    fun addReview(
        applicationId: Long,
        reviewerUserId: Long,
        reviewerRole: String,
        status: String,
        comments: String?
    ) {

        val sql = """
            INSERT INTO role_application_reviews (
                application_id,
                reviewer_user_id,
                reviewer_role,
                status,
                comments,
                reviewed_at
            )
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, applicationId)
            statement.setLong(2, reviewerUserId)
            statement.setString(3, reviewerRole)
            statement.setString(4, status)
            statement.setString(5, comments)

            statement.executeUpdate()
        }
    }

    fun getApplication(
        applicationId: Long
    ): RoleApplicationDetails? {

        val sql = """
            SELECT
                application_id,
                role,
                name,
                email,
                phone,
                password_hash,
                profile_picture_url,
                college_id,
                college_id_number,
                college_id_photo_url,
                designation,
                requested_department_id,
                requested_organization_id,
                proposed_college_name,
                proposed_college_code,
                proposed_email_domain,
                proposed_address,
                proposed_city,
                proposed_latitude,
                proposed_longitude,
                proposed_website,
                proposed_logo_url,
                status
            FROM role_applications
            WHERE application_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, applicationId)

            statement.executeQuery().use { result ->

                if (!result.next()) {
                    return null
                }

                return RoleApplicationDetails(
                    applicationId =
                        result.getLong("application_id"),

                    role =
                        result.getString("role"),

                    name =
                        result.getString("name"),

                    email =
                        result.getString("email"),

                    phone =
                        result.getString("phone"),

                    passwordHash =
                        result.getString("password_hash"),

                    profilePictureUrl =
                        result.getString("profile_picture_url"),

                    collegeId =
                        result.getLong("college_id").let {
                            if (result.wasNull()) null else it
                        },

                    collegeIdNumber =
                        result.getString("college_id_number"),

                    collegeIdPhotoUrl =
                        result.getString("college_id_photo_url"),

                    designation =
                        result.getString("designation"),

                    requestedDepartmentId =
                        result.getLong(
                            "requested_department_id"
                        ).let {
                            if (result.wasNull()) null else it
                        },

                    requestedOrganizationId =
                        result.getLong(
                            "requested_organization_id"
                        ).let {
                            if (result.wasNull()) null else it
                        },

                    proposedCollegeName =
                        result.getString("proposed_college_name"),

                    proposedCollegeCode =
                        result.getString("proposed_college_code"),

                    proposedEmailDomain =
                        result.getString("proposed_email_domain"),

                    proposedAddress =
                        result.getString("proposed_address"),

                    proposedCity =
                        result.getString("proposed_city"),

                    proposedLatitude =
                        result.getDouble("proposed_latitude").let {
                            if (result.wasNull()) null else it
                        },

                    proposedLongitude =
                        result.getDouble("proposed_longitude").let {
                            if (result.wasNull()) null else it
                        },

                    proposedWebsite =
                        result.getString("proposed_website"),

                    proposedLogoUrl =
                        result.getString("proposed_logo_url"),

                    status =
                        result.getString("status")
                )
            }
        }
    }

    fun approveApplication(
        applicationId: Long
    ): Long {

        connection.autoCommit = false

        try {

            val application =
                getApplication(applicationId)
                    ?: throw IllegalArgumentException(
                        "Application not found"
                    )

            if (application.status != "PENDING") {
                throw IllegalStateException(
                    "Application has already been processed"
                )
            }

            var finalCollegeId =
                application.collegeId

            /*
             * College Admin applying for a new college.
             */
            if (
                application.role == "COLLEGE_ADMIN" &&
                finalCollegeId == null
            ) {

                val collegeSql = """
                    INSERT INTO colleges (
                        college_name,
                        college_code,
                        email_domain,
                        city,
                        address,
                        latitude,
                        longitude,
                        website,
                        logo_url
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING college_id
                """.trimIndent()

                connection.prepareStatement(collegeSql).use { statement ->

                    statement.setString(
                        1,
                        application.proposedCollegeName
                    )

                    statement.setString(
                        2,
                        application.proposedCollegeCode
                    )

                    statement.setString(
                        3,
                        application.proposedEmailDomain
                    )

                    statement.setString(
                        4,
                        application.proposedCity
                    )

                    statement.setString(
                        5,
                        application.proposedAddress
                    )

                    if (application.proposedLatitude != null) {
                        statement.setDouble(
                            6,
                            application.proposedLatitude
                        )
                    } else {
                        statement.setNull(
                            6,
                            java.sql.Types.DOUBLE
                        )
                    }

                    if (application.proposedLongitude != null) {
                        statement.setDouble(
                            7,
                            application.proposedLongitude
                        )
                    } else {
                        statement.setNull(
                            7,
                            java.sql.Types.DOUBLE
                        )
                    }

                    statement.setString(
                        8,
                        application.proposedWebsite
                    )

                    statement.setString(
                        9,
                        application.proposedLogoUrl
                    )

                    statement.executeQuery().use { result ->

                        result.next()

                        finalCollegeId =
                            result.getLong("college_id")
                    }
                }
            }

            /*
             * Create the actual user.
             */
            val userSql = """
                INSERT INTO users (
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
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, 'ACTIVE')
                RETURNING user_id
            """.trimIndent()

            val userId: Long

            connection.prepareStatement(userSql).use { statement ->

                statement.setString(
                    1,
                    application.name
                )

                statement.setString(
                    2,
                    application.email
                )

                statement.setString(
                    3,
                    application.passwordHash
                )

                statement.setString(
                    4,
                    application.phone
                )

                statement.setString(
                    5,
                    application.profilePictureUrl
                )

                if (finalCollegeId != null) {
                    statement.setLong(
                        6,
                        finalCollegeId
                    )
                } else {
                    statement.setNull(
                        6,
                        java.sql.Types.BIGINT
                    )
                }

                statement.setString(
                    7,
                    application.role
                )

                statement.setString(
                    8,
                    application.designation
                )

                statement.executeQuery().use { result ->

                    result.next()

                    userId =
                        result.getLong("user_id")
                }
            }

            /*
             * Add department affiliation.
             */
            if (application.requestedDepartmentId != null) {

                val sql = """
                    INSERT INTO user_departments (
                        user_id,
                        department_id
                    )
                    VALUES (?, ?)
                """.trimIndent()

                connection.prepareStatement(sql).use { statement ->

                    statement.setLong(1, userId)

                    statement.setLong(
                        2,
                        application.requestedDepartmentId
                    )

                    statement.executeUpdate()
                }
            }

            /*
             * Add organization affiliation.
             */
            if (application.requestedOrganizationId != null) {

                val sql = """
                    INSERT INTO user_organizations (
                        user_id,
                        organization_id
                    )
                    VALUES (?, ?)
                """.trimIndent()

                connection.prepareStatement(sql).use { statement ->

                    statement.setLong(1, userId)

                    statement.setLong(
                        2,
                        application.requestedOrganizationId
                    )

                    statement.executeUpdate()
                }
            }

            /*
             * Mark application as approved.
             */
            val updateSql = """
                UPDATE role_applications
                SET status = 'APPROVED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE application_id = ?
            """.trimIndent()

            connection.prepareStatement(updateSql).use { statement ->

                statement.setLong(1, applicationId)

                statement.executeUpdate()
            }

            connection.commit()

            return userId

        } catch (exception: Exception) {

            connection.rollback()

            throw exception

        } finally {

            connection.autoCommit = true
        }
    }

    fun rejectApplication(
        applicationId: Long,
        reason: String
    ) {

        val sql = """
            UPDATE role_applications
            SET status = 'REJECTED',
                rejection_reason = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE application_id = ?
              AND status = 'PENDING'
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setString(1, reason)
            statement.setLong(2, applicationId)

            val rowsUpdated =
                statement.executeUpdate()

            if (rowsUpdated == 0) {
                throw IllegalStateException(
                    "Application not found or already processed"
                )
            }
        }
    }
}