package com.eventhandling

import java.sql.Connection

class CollegeLookupService(
    private val connection: Connection
) {

    fun getAllColleges(): List<CollegeLookup> {

        val sql = """
            SELECT
                college_id,
                college_name,
                college_code,
                city,
                address,
                website,
                logo_url
            FROM colleges
            ORDER BY college_name
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.executeQuery().use { result ->

                val colleges = mutableListOf<CollegeLookup>()

                while (result.next()) {
                    colleges.add(
                        CollegeLookup(
                            collegeId =
                                result.getLong("college_id"),
                            collegeName =
                                result.getString("college_name"),
                            collegeCode =
                                result.getString("college_code"),
                            city =
                                result.getString("city"),
                            address =
                                result.getString("address"),
                            website =
                                result.getString("website"),
                            logoUrl =
                                result.getString("logo_url")
                        )
                    )
                }

                return colleges
            }
        }
    }

    fun getDepartments(collegeId: Long): List<DepartmentLookup> {

        val sql = """
            SELECT
                department_id,
                department_name,
                department_code
            FROM departments
            WHERE college_id = ?
            ORDER BY department_name
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, collegeId)

            statement.executeQuery().use { result ->

                val departments = mutableListOf<DepartmentLookup>()

                while (result.next()) {
                    departments.add(
                        DepartmentLookup(
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

    fun getOrganizations(collegeId: Long): List<OrganizationLookup> {

        val sql = """
            SELECT
                organization_id,
                organization_name,
                organization_type
            FROM organizations
            WHERE college_id = ?
            ORDER BY organization_name
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->

            statement.setLong(1, collegeId)

            statement.executeQuery().use { result ->

                val organizations = mutableListOf<OrganizationLookup>()

                while (result.next()) {
                    organizations.add(
                        OrganizationLookup(
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