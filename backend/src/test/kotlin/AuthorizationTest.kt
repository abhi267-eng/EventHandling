package com.eventhandling

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationTest {

    private fun user(id: Long, role: String, collegeId: Long?) = User(
        userId = id,
        name = "User $id",
        email = "user$id@test.com",
        phone = null,
        profilePictureUrl = null,
        collegeId = collegeId,
        role = role,
        designation = null,
        emailVerified = true,
        accountStatus = "ACTIVE"
    )

    @Test
    fun `nobody can manage themselves`() {
        val admin = user(1, "PLATFORM_ADMIN", null)

        assertFalse(canManage(admin, admin))
    }

    @Test
    fun `platform admin can manage a college admin of any college`() {
        val platformAdmin = user(1, "PLATFORM_ADMIN", null)
        val collegeAdmin = user(2, "COLLEGE_ADMIN", 10)

        assertTrue(canManage(platformAdmin, collegeAdmin))
    }

    @Test
    fun `platform admin can manage a normal user`() {
        val platformAdmin = user(1, "PLATFORM_ADMIN", null)
        val normalUser = user(2, "NORMAL_USER", null)

        assertTrue(canManage(platformAdmin, normalUser))
    }

    @Test
    fun `college admin can manage lower roles in the same college`() {
        val collegeAdmin = user(1, "COLLEGE_ADMIN", 10)

        assertTrue(canManage(collegeAdmin, user(2, "FACULTY_COORDINATOR", 10)))
        assertTrue(canManage(collegeAdmin, user(3, "EVENT_COORDINATOR", 10)))
        assertTrue(canManage(collegeAdmin, user(4, "STUDENT_ORGANIZER", 10)))
        assertTrue(canManage(collegeAdmin, user(5, "NORMAL_USER", 10)))
    }

    @Test
    fun `college admin cannot manage users of another college`() {
        val collegeAdmin = user(1, "COLLEGE_ADMIN", 10)
        val otherCollegeUser = user(2, "FACULTY_COORDINATOR", 20)

        assertFalse(canManage(collegeAdmin, otherCollegeUser))
    }

    @Test
    fun `college admin cannot manage users without a college`() {
        val collegeAdmin = user(1, "COLLEGE_ADMIN", 10)
        val noCollegeUser = user(2, "NORMAL_USER", null)

        assertFalse(canManage(collegeAdmin, noCollegeUser))
    }

    @Test
    fun `college admin without a college cannot manage anyone`() {
        val collegeAdmin = user(1, "COLLEGE_ADMIN", null)
        val normalUser = user(2, "NORMAL_USER", null)

        assertFalse(canManage(collegeAdmin, normalUser))
    }

    @Test
    fun `college admin cannot manage a peer or a platform admin`() {
        val collegeAdmin = user(1, "COLLEGE_ADMIN", 10)

        assertFalse(canManage(collegeAdmin, user(2, "COLLEGE_ADMIN", 10)))
        assertFalse(canManage(collegeAdmin, user(3, "PLATFORM_ADMIN", null)))
    }

    @Test
    fun `platform admin cannot be managed by anyone else`() {
        val platformAdmin = user(1, "PLATFORM_ADMIN", null)
        val otherPlatformAdmin = user(2, "PLATFORM_ADMIN", null)

        assertFalse(canManage(platformAdmin, otherPlatformAdmin))
    }

    @Test
    fun `coordinators and organizers cannot manage anyone`() {
        val target = user(9, "NORMAL_USER", 10)

        assertFalse(canManage(user(1, "FACULTY_COORDINATOR", 10), target))
        assertFalse(canManage(user(2, "EVENT_COORDINATOR", 10), target))
        assertFalse(canManage(user(3, "STUDENT_ORGANIZER", 10), target))
        assertFalse(canManage(user(4, "NORMAL_USER", 10), target))
    }

    @Test
    fun `unknown roles are never allowed`() {
        val mystery = user(1, "SOMETHING_ELSE", 10)
        val normalUser = user(2, "NORMAL_USER", 10)

        assertFalse(canManage(mystery, normalUser))
    }
}