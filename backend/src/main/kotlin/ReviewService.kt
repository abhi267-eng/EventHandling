package com.eventhandling

import kotlinx.serialization.Serializable
import java.sql.Connection

@Serializable
data class ReviewRequest(
    val rating: Int,
    val reviewText: String? = null
)

class ReviewService(private val connection: Connection) {

    // Returns NOT_ENDED, OPEN or CLOSED (null if the event does not exist)
    fun getReviewState(eventId: Long): String? {
        val sql = """
            SELECT CASE
                WHEN LOCALTIMESTAMP < end_datetime THEN 'NOT_ENDED'
                WHEN LOCALTIMESTAMP > end_datetime + INTERVAL '24 hours' THEN 'CLOSED'
                ELSE 'OPEN'
            END AS review_state
            FROM events
            WHERE event_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, eventId)

            statement.executeQuery().use { result ->
                return if (result.next()) result.getString("review_state") else null
            }
        }
    }

    // Returns true if a new review was saved, false if this user already reviewed the event
    fun addReview(eventId: Long, userId: Long, rating: Int, reviewText: String?): Boolean {
        val sql = """
            INSERT INTO event_reviews (event_id, user_id, rating, review_text)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, event_id) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, eventId)
            statement.setLong(2, userId)
            statement.setInt(3, rating)
            statement.setString(4, reviewText)
            return statement.executeUpdate() > 0
        }
    }
}