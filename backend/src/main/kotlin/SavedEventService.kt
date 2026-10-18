package com.eventhandling

import java.sql.Connection

class SavedEventService(private val connection: Connection) {

    fun saveEvent(userId: Long, eventId: Long) {
        val sql = """
            INSERT INTO saved_events (user_id, event_id)
            VALUES (?, ?)
            ON CONFLICT (user_id, event_id) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.setLong(2, eventId)
            statement.executeUpdate()
        }
    }

    fun unsaveEvent(userId: Long, eventId: Long): Boolean {
        val sql = "DELETE FROM saved_events WHERE user_id = ? AND event_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.setLong(2, eventId)
            return statement.executeUpdate() > 0
        }
    }

    fun getSavedEvents(userId: Long, viewerCollegeId: Long?): List<Event> {
        val events = mutableListOf<Event>()

        val sql = """
            SELECT
                e.event_id,
                e.title,
                e.description,
                e.college_id,
                e.created_by,
                e.category_id,
                e.rules,
                e.start_datetime,
                e.end_datetime,
                e.venue_type,
                e.venue_name,
                e.venue_address,
                e.latitude,
                e.longitude,
                e.accept_visitors,
                e.status,
                e.verification_status,
                e.poster_url
            FROM saved_events s
            JOIN events e ON e.event_id = s.event_id
            WHERE s.user_id = ?
              AND e.status = 'APPROVED'
              AND (e.accept_visitors = TRUE OR e.college_id = ?)
            ORDER BY s.saved_at DESC
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.setObject(2, viewerCollegeId, java.sql.Types.BIGINT)

            statement.executeQuery().use { result ->
                while (result.next()) {
                    events.add(
                        Event(
                            eventId = result.getLong("event_id"),
                            title = result.getString("title"),
                            description = result.getString("description"),
                            collegeId = result.getLong("college_id"),
                            createdBy = result.getObject("created_by")?.let { (it as Number).toLong() },
                            categoryId = result.getObject("category_id")?.let { (it as Number).toLong() },
                            rules = result.getString("rules"),
                            startDatetime = result.getString("start_datetime"),
                            endDatetime = result.getString("end_datetime"),
                            venueType = result.getString("venue_type"),
                            venueName = result.getString("venue_name"),
                            venueAddress = result.getString("venue_address"),
                            latitude = result.getObject("latitude")?.let { (it as Number).toDouble() },
                            longitude = result.getObject("longitude")?.let { (it as Number).toDouble() },
                            acceptVisitors = result.getBoolean("accept_visitors"),
                            status = result.getString("status"),
                            verificationStatus = result.getString("verification_status"),
                            posterUrl = result.getString("poster_url")
                        )
                    )
                }
            }
        }

        return events
    }
}