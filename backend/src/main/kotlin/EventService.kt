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
    fun getEventById(eventId: Long): Event? {

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
        WHERE event_id = ?
    """.trimIndent()

    connection.prepareStatement(query).use { statement ->

        statement.setLong(1, eventId)

        statement.executeQuery().use { result ->

            if (result.next()) {
                return Event(
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
            }
        }
    }

    return null
}
fun createEvent(request: CreateEventRequest): Event {

    val query = """
        INSERT INTO events (
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
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 'UNVERIFIED', ?)
        RETURNING
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
    """.trimIndent()

    connection.prepareStatement(query).use { statement ->

        statement.setString(1, request.title)
        statement.setString(2, request.description)
        statement.setLong(3, request.collegeId)

        if (request.createdBy != null) {
            statement.setLong(4, request.createdBy)
        } else {
            statement.setNull(4, java.sql.Types.BIGINT)
        }

        if (request.categoryId != null) {
            statement.setLong(5, request.categoryId)
        } else {
            statement.setNull(5, java.sql.Types.BIGINT)
        }

        statement.setString(6, request.rules)
        statement.setTimestamp(
            7,
            java.sql.Timestamp.valueOf(request.startDatetime)
        )

        statement.setTimestamp(
            8,
            java.sql.Timestamp.valueOf(request.endDatetime)
        )
        statement.setString(9, request.venueType)
        statement.setString(10, request.venueName)
        statement.setString(11, request.venueAddress)

        if (request.latitude != null) {
            statement.setDouble(12, request.latitude)
        } else {
            statement.setNull(12, java.sql.Types.DOUBLE)
        }

        if (request.longitude != null) {
            statement.setDouble(13, request.longitude)
        } else {
            statement.setNull(13, java.sql.Types.DOUBLE)
        }

        statement.setBoolean(14, request.acceptVisitors)
        statement.setString(15, request.posterUrl)

        statement.executeQuery().use { result ->
            result.next()

            return Event(
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
        }
    }
}
fun updateEvent(eventId: Long, request: CreateEventRequest): Event? {

    val query = """
        UPDATE events
        SET
            title = ?,
            description = ?,
            college_id = ?,
            created_by = ?,
            category_id = ?,
            rules = ?,
            start_datetime = ?,
            end_datetime = ?,
            venue_type = ?,
            venue_name = ?,
            venue_address = ?,
            latitude = ?,
            longitude = ?,
            accept_visitors = ?,
            poster_url = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE event_id = ?
        RETURNING
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
    """.trimIndent()

    connection.prepareStatement(query).use { statement ->

        statement.setString(1, request.title)
        statement.setString(2, request.description)
        statement.setLong(3, request.collegeId)

        if (request.createdBy != null) {
            statement.setLong(4, request.createdBy)
        } else {
            statement.setNull(4, java.sql.Types.BIGINT)
        }

        if (request.categoryId != null) {
            statement.setLong(5, request.categoryId)
        } else {
            statement.setNull(5, java.sql.Types.BIGINT)
        }

        statement.setString(6, request.rules)

        statement.setTimestamp(
            7,
            java.sql.Timestamp.valueOf(request.startDatetime)
        )

        statement.setTimestamp(
            8,
            java.sql.Timestamp.valueOf(request.endDatetime)
        )

        statement.setString(9, request.venueType)
        statement.setString(10, request.venueName)
        statement.setString(11, request.venueAddress)

        if (request.latitude != null) {
            statement.setDouble(12, request.latitude)
        } else {
            statement.setNull(12, java.sql.Types.DOUBLE)
        }

        if (request.longitude != null) {
            statement.setDouble(13, request.longitude)
        } else {
            statement.setNull(13, java.sql.Types.DOUBLE)
        }

        statement.setBoolean(14, request.acceptVisitors)
        statement.setString(15, request.posterUrl)

        statement.setLong(16, eventId)

        statement.executeQuery().use { result ->

            if (!result.next()) {
                return null
            }

            return Event(
                eventId = result.getLong("event_id"),
                title = result.getString("title"),
                description = result.getString("description"),
                collegeId = result.getLong("college_id"),
                createdBy = result.getObject("created_by")
                    ?.let { (it as Number).toLong() },
                categoryId = result.getObject("category_id")
                    ?.let { (it as Number).toLong() },
                rules = result.getString("rules"),
                startDatetime = result.getString("start_datetime"),
                endDatetime = result.getString("end_datetime"),
                venueType = result.getString("venue_type"),
                venueName = result.getString("venue_name"),
                venueAddress = result.getString("venue_address"),
                latitude = result.getObject("latitude")
                    ?.let { (it as Number).toDouble() },
                longitude = result.getObject("longitude")
                    ?.let { (it as Number).toDouble() },
                acceptVisitors = result.getBoolean("accept_visitors"),
                status = result.getString("status"),
                verificationStatus = result.getString("verification_status"),
                posterUrl = result.getString("poster_url")
            )
        }
    }
}
fun patchEvent(eventId: Long, request: PatchEventRequest): Event? {

    val fields = mutableListOf<String>()
    val values = mutableListOf<Any?>()

    request.title?.let {
        fields.add("title = ?")
        values.add(it)
    }

    request.description?.let {
        fields.add("description = ?")
        values.add(it)
    }

    request.collegeId?.let {
        fields.add("college_id = ?")
        values.add(it)
    }

    request.createdBy?.let {
        fields.add("created_by = ?")
        values.add(it)
    }

    request.categoryId?.let {
        fields.add("category_id = ?")
        values.add(it)
    }

    request.rules?.let {
        fields.add("rules = ?")
        values.add(it)
    }

    request.startDatetime?.let {
        fields.add("start_datetime = ?")
        values.add(java.sql.Timestamp.valueOf(it))
    }

    request.endDatetime?.let {
        fields.add("end_datetime = ?")
        values.add(java.sql.Timestamp.valueOf(it))
    }

    request.venueType?.let {
        fields.add("venue_type = ?")
        values.add(it)
    }

    request.venueName?.let {
        fields.add("venue_name = ?")
        values.add(it)
    }

    request.venueAddress?.let {
        fields.add("venue_address = ?")
        values.add(it)
    }

    request.latitude?.let {
        fields.add("latitude = ?")
        values.add(it)    }

    request.longitude?.let {
        fields.add("longitude = ?")
        values.add(it)
    }

    request.acceptVisitors?.let {
        fields.add("accept_visitors = ?")
        values.add(it)
    }

    request.posterUrl?.let {
        fields.add("poster_url = ?")
        values.add(it)
    }

    if (fields.isEmpty()) {
        return getEventById(eventId)
    }

    fields.add("updated_at = CURRENT_TIMESTAMP")

    val query = """
        UPDATE events
        SET ${fields.joinToString(", ")}
        WHERE event_id = ?
        RETURNING
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
    """.trimIndent()

    connection.prepareStatement(query).use { statement ->

        values.forEachIndexed { index, value ->
            val parameterIndex = index + 1

            when (value) {
                null -> statement.setNull(
                    parameterIndex,
                    java.sql.Types.NULL
                )

                is String -> statement.setString(
                    parameterIndex,
                    value
                )

                is Long -> statement.setLong(
                    parameterIndex,
                    value
                )

                is Double -> statement.setDouble(
                    parameterIndex,
                    value
                )

                is Boolean -> statement.setBoolean(
                    parameterIndex,
                    value
                )

                is java.sql.Timestamp -> statement.setTimestamp(
                    parameterIndex,
                    value
                )
            }
        }

        statement.setLong(values.size + 1, eventId)

        statement.executeQuery().use { result ->

            if (!result.next()) {
                return null
            }

            return Event(
                eventId = result.getLong("event_id"),
                title = result.getString("title"),
                description = result.getString("description"),
                collegeId = result.getLong("college_id"),
                createdBy = result.getObject("created_by")
                    ?.let { (it as Number).toLong() },
                categoryId = result.getObject("category_id")
                    ?.let { (it as Number).toLong() },
                rules = result.getString("rules"),
                startDatetime = result.getString("start_datetime"),
                endDatetime = result.getString("end_datetime"),
                venueType = result.getString("venue_type"),
                venueName = result.getString("venue_name"),
                venueAddress = result.getString("venue_address"),
                latitude = result.getObject("latitude")
                    ?.let { (it as Number).toDouble() },
                longitude = result.getObject("longitude")
                    ?.let { (it as Number).toDouble() },
                acceptVisitors = result.getBoolean("accept_visitors"),
                status = result.getString("status"),
                verificationStatus = result.getString("verification_status"),
                posterUrl = result.getString("poster_url")
            )
        }
    }
}
fun deleteEvent(eventId: Long): Boolean {

    val query = """
        DELETE FROM events
        WHERE event_id = ?
    """.trimIndent()

    connection.prepareStatement(query).use { statement ->

        statement.setLong(1, eventId)

        val rowsDeleted = statement.executeUpdate()

        return rowsDeleted > 0
    }
}
}