package com.eventhandling

import java.sql.Connection

class EventService(private val connection: Connection) {

    fun getAllEvents(): List<Event> {
        val events = mutableListOf<Event>()

        val query = """
            SELECT
                event_id,
                title,
                description,
                college_id,
                created_by,
                category_id,
                rules,
                start_datetime,
                end_datetime,
                venue_type,
                venue_name,
                venue_address,
                latitude,
                longitude,
                accept_visitors,
                status,
                verification_status,
                poster_url
            FROM events
            ORDER BY start_datetime
        """.trimIndent()

        connection.prepareStatement(query).use { statement ->
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