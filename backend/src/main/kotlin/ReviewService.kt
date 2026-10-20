package com.eventhandling

import kotlinx.serialization.Serializable
import java.sql.Connection

@Serializable
data class ReviewRequest(
    val rating: Int,
    val reviewText: String? = null
)

@Serializable
data class ReviewItem(
    val reviewId: Long,
    val rating: Int,
    val reviewText: String?,
    val reviewerName: String,
    val createdAt: String
)

@Serializable
data class EventReviewsResponse(
    val averageRating: Double?,
    val reviewCount: Int,
    val reviews: List<ReviewItem>
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

    fun getReviews(eventId: Long): EventReviewsResponse {
        val reviews = mutableListOf<ReviewItem>()

        val sql = """
            SELECT r.review_id, r.rating, r.review_text, r.created_at, u.name
            FROM event_reviews r
            JOIN users u ON u.user_id = r.user_id
            WHERE r.event_id = ?
            ORDER BY r.created_at DESC
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, eventId)

            statement.executeQuery().use { result ->
                while (result.next()) {
                    reviews.add(
                        ReviewItem(
                            reviewId = result.getLong("review_id"),
                            rating = result.getInt("rating"),
                            reviewText = result.getString("review_text"),
                            reviewerName = result.getString("name"),
                            createdAt = result.getString("created_at")
                        )
                    )
                }
            }
        }

        val average = if (reviews.isEmpty()) {
            null
        } else {
            Math.round(reviews.map { it.rating }.average() * 10) / 10.0
        }

        return EventReviewsResponse(average, reviews.size, reviews)
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