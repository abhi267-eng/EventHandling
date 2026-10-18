package com.eventhandling

import java.sql.Connection
import kotlinx.serialization.Serializable

@Serializable
data class PendingEvent(
    val eventId: Long,
    val collegeId: Long,
    val createdBy: Long?,
    val title: String,
    val categoryId: Long,
    val startDatetime: String,
    val endDatetime: String,
    val venueType: String,
    val venueName: String,
    val acceptVisitors: Boolean,
    val status: String
)

class AdminQueueService(
    private val connection: Connection
) {

    /*
     * Which application roles each reviewer role may review.
     * Same hierarchy as /auth/applications/review.
     */
    private val rolesReviewedBy = mapOf(
        "PLATFORM_ADMIN" to listOf("COLLEGE_ADMIN"),
        "COLLEGE_ADMIN" to listOf("FACULTY_COORDINATOR", "EVENT_COORDINATOR"),
        "FACULTY_COORDINATOR" to listOf("STUDENT_ORGANIZER"),
        "EVENT_COORDINATOR" to listOf("STUDENT_ORGANIZER")
    )

    fun getPendingApplications(
        reviewerRole: String,
        reviewerCollegeId: Long?
    ): List<RoleApplication> {

        val roles = rolesReviewedBy[reviewerRole] ?: return emptyList()
        val limitedToCollege = reviewerRole != "PLATFORM_ADMIN"

        if (limitedToCollege && reviewerCollegeId == null) {
            return emptyList()
        }

        val placeholders = roles.joinToString(", ") { "?" }
        val collegeFilter = if (limitedToCollege) "AND college_id = ?" else ""

        val sql = """
            SELECT application_id, role, name, email, phone,
                   profile_picture_url, college_id, college_id_number,
                   college_id_photo_url, designation,
                   requested_department_id, requested_organization_id,
                   status
            FROM role_applications
            WHERE status = 'PENDING'
              AND role IN ($placeholders)
              $collegeFilter
            ORDER BY created_at
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            roles.forEachIndexed { index, role ->
                statement.setString(index + 1, role)
            }

            if (limitedToCollege) {
                statement.setLong(roles.size + 1, reviewerCollegeId!!)
            }

            statement.executeQuery().use { result ->

                val applications = mutableListOf<RoleApplication>()

                while (result.next()) {
                    applications += RoleApplication(
                        applicationId = result.getLong("application_id"),
                        role = result.getString("role"),
                        name = result.getString("name"),
                        email = result.getString("email"),
                        phone = result.getString("phone"),
                        profilePictureUrl = result.getString("profile_picture_url"),
                        collegeId = result.getLong("college_id").let {
                            if (result.wasNull()) null else it
                        },
                        collegeIdNumber = result.getString("college_id_number"),
                        collegeIdPhotoUrl = result.getString("college_id_photo_url"),
                        designation = result.getString("designation"),
                        requestedDepartmentId = result.getLong("requested_department_id").let {
                            if (result.wasNull()) null else it
                        },
                        requestedOrganizationId = result.getLong("requested_organization_id").let {
                            if (result.wasNull()) null else it
                        },
                        status = result.getString("status")
                    )
                }

                return applications
            }
        }
    }

    fun getPendingEvents(
        collegeId: Long?,
        limit: Int,
        offset: Int
    ): List<PendingEvent> {

        val collegeFilter = if (collegeId != null) "AND college_id = ?" else ""

        val sql = """
            SELECT event_id, college_id, created_by, title, category_id,
                   start_datetime, end_datetime, venue_type, venue_name,
                   accept_visitors, status
            FROM events
            WHERE status = 'PENDING'
              $collegeFilter
            ORDER BY created_at
            LIMIT ? OFFSET ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            var index = 1

            if (collegeId != null) {
                statement.setLong(index++, collegeId)
            }

            statement.setInt(index++, limit)
            statement.setInt(index, offset)

            statement.executeQuery().use { result ->

                val events = mutableListOf<PendingEvent>()

                while (result.next()) {
                    events += PendingEvent(
                        eventId = result.getLong("event_id"),
                        collegeId = result.getLong("college_id"),
                        createdBy = result.getLong("created_by").let {
                            if (result.wasNull()) null else it
                        },
                        title = result.getString("title"),
                        categoryId = result.getLong("category_id"),
                        startDatetime = result.getString("start_datetime"),
                        endDatetime = result.getString("end_datetime"),
                        venueType = result.getString("venue_type"),
                        venueName = result.getString("venue_name"),
                        acceptVisitors = result.getBoolean("accept_visitors"),
                        status = result.getString("status")
                    )
                }

                return events
            }
        }
    }
}