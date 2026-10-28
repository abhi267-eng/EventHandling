package com.eventhandling

import kotlinx.serialization.Serializable
import java.sql.Connection

@Serializable
data class CreateDepartmentRequest(
    val departmentName: String,
    val departmentCode: String? = null
)

@Serializable
data class CreateOrganizationRequest(
    val organizationName: String,
    val organizationType: String
)

class CollegeManagementService(
    private val connection: Connection
) {

    fun addDepartment(collegeId: Long, name: String, code: String?): DepartmentLookup {

        val sql = """
            INSERT INTO departments (college_id, department_name, department_code)
            VALUES (?, ?, ?)
            RETURNING department_id, department_name, department_code
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, collegeId)
            statement.setString(2, name)
            statement.setString(3, code)

            statement.executeQuery().use { result ->
                result.next()
                return DepartmentLookup(
                    departmentId = result.getLong("department_id"),
                    departmentName = result.getString("department_name"),
                    departmentCode = result.getString("department_code")
                )
            }
        }
    }

    fun getDepartmentCollegeId(departmentId: Long): Long? {
        val sql = "SELECT college_id FROM departments WHERE department_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, departmentId)
            statement.executeQuery().use { result ->
                return if (result.next()) result.getLong("college_id") else null
            }
        }
    }

    fun deleteDepartment(departmentId: Long): Boolean {
        val sql = "DELETE FROM departments WHERE department_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, departmentId)
            return statement.executeUpdate() > 0
        }
    }

    fun addOrganization(collegeId: Long, name: String, type: String): OrganizationLookup {

        val sql = """
            INSERT INTO organizations (college_id, organization_name, organization_type)
            VALUES (?, ?, ?)
            RETURNING organization_id, organization_name, organization_type
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, collegeId)
            statement.setString(2, name)
            statement.setString(3, type)

            statement.executeQuery().use { result ->
                result.next()
                return OrganizationLookup(
                    organizationId = result.getLong("organization_id"),
                    organizationName = result.getString("organization_name"),
                    organizationType = result.getString("organization_type")
                )
            }
        }
    }

    fun getOrganizationCollegeId(organizationId: Long): Long? {
        val sql = "SELECT college_id FROM organizations WHERE organization_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, organizationId)
            statement.executeQuery().use { result ->
                return if (result.next()) result.getLong("college_id") else null
            }
        }
    }

    fun deleteOrganization(organizationId: Long): Boolean {
        val sql = "DELETE FROM organizations WHERE organization_id = ?"

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, organizationId)
            return statement.executeUpdate() > 0
        }
    }
}