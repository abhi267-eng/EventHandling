package com.eventhandling

import java.sql.Connection

class EventCategoryService(
    private val connection: Connection
) {

    fun getAllCategories(): List<CategoryLookup> {

        val sql = """
            SELECT
                category_id,
                category_name,
                description
            FROM event_categories
            ORDER BY category_id
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.executeQuery().use { result ->

                val categories = mutableListOf<CategoryLookup>()

                while (result.next()) {
                    categories.add(
                        CategoryLookup(
                            categoryId =
                                result.getLong("category_id"),
                            categoryName =
                                result.getString("category_name"),
                            description =
                                result.getString("description")
                        )
                    )
                }

                return categories
            }
        }
    }
}