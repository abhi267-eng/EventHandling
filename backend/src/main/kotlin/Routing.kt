package com.eventhandling

import java.sql.SQLException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.response.respondText

fun Application.configureRouting() {

    val connection = connectToPostgres(embedded = false)
    val eventService = EventService(connection)
    val adminService = AdminService(connection)
    val adminQueueService = AdminQueueService(connection)
    val collegeManagementService = CollegeManagementService(connection)

    routing {

        get("/") {
            call.respondText("Hello, World!")
        }

        post("/auth/signup") {

            val request = call.receive<SignupRequest>()

            val validationError = validateSignupRequest(request)

            if (validationError != null) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    validationError
                )
                return@post
            }

            val userService = UserService(connection)

            if (userService.emailExists(request.email)) {
                call.respond(
                    io.ktor.http.HttpStatusCode.Conflict,
                    "Email already registered"
                )
                return@post
            }

            val user = userService.createNormalUser(request)

            call.respond(
                io.ktor.http.HttpStatusCode.Created,
                user
            )
        }

        post("/auth/apply") {

            val request =
                call.receive<RoleApplicationRequest>()

            val validationError =
                validateRoleApplication(request)

            if (validationError != null) {

                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    validationError
                )

                return@post
            }

            val userService =
                UserService(connection)

            if (userService.emailExists(request.email)) {

                call.respond(
                    io.ktor.http.HttpStatusCode.Conflict,
                    "Email already registered"
                )

                return@post
            }

            val roleApplicationService =
                RoleApplicationService(connection)

            if (
                roleApplicationService.pendingApplicationExists(
                    request.email,
                    request.role
                )
            ) {

                call.respond(
                    io.ktor.http.HttpStatusCode.Conflict,
                    "A pending application already exists for this email and role"
                )

                return@post
            }

            val application =
                roleApplicationService.createApplication(request)

            call.respond(
                io.ktor.http.HttpStatusCode.Created,
                application
            )
        }

        post("/auth/login") {

            val request = call.receive<LoginRequest>()

            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Email and password are required"
                )
                return@post
            }

            val authService = AuthService(connection)

            try {

                val user = authService.login(request)

                if (user == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Invalid email or password"
                    )
                    return@post
                }

                val token = JwtService.generateToken(user)

                call.respond(
                    io.ktor.http.HttpStatusCode.OK,
                    LoginResponse(
                        message = "Login successful",
                        user = user,
                        token = token
                    )
                )

            } catch (exception: IllegalStateException) {

                call.respond(
                    io.ktor.http.HttpStatusCode.Forbidden,
                    exception.message ?: "Login not allowed"
                )
            }
        }

        post("/auth/verify-email") {

            val request = call.receive<VerifyEmailRequest>()

            val emailVerificationService = EmailVerificationService(connection)

            val verified = emailVerificationService.verifyCode(
                request.userId,
                request.code
            )

            if (!verified) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Invalid or expired verification code"
                )
                return@post
            }

            call.respond(
                io.ktor.http.HttpStatusCode.OK,
                "Email verified successfully"
            )
        }

        post("/setup/platform-admin") {

            val setupSecret =
                environment.config
                    .property("setup.platform-admin-secret")
                    .getString()

            val request =
                call.receive<PlatformAdminBootstrapRequest>()

            if (request.setupSecret != setupSecret) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid setup secret"
                )
                return@post
            }

            val service =
                PlatformAdminBootstrapService(connection)

            if (service.platformAdminExists()) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Platform Admin already exists"
                )
                return@post
            }

            val user =
                service.createPlatformAdmin(
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    phone = request.phone,
                    profilePictureUrl = request.profilePictureUrl
                )

            call.respond(
                HttpStatusCode.Created,
                user
            )
        }

        // Public college and affiliation lookup

        get("/colleges") {

            val collegeLookupService =
                CollegeLookupService(connection)

            val colleges =
                collegeLookupService.getAllColleges()

            call.respond(colleges)
        }

        get("/colleges/{id}/departments") {

            val collegeId =
                call.parameters["id"]?.toLongOrNull()

            if (collegeId == null) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Invalid college ID"
                )
                return@get
            }

            val collegeLookupService =
                CollegeLookupService(connection)

            val departments =
                collegeLookupService.getDepartments(collegeId)

            call.respond(departments)
        }

        get("/colleges/{id}/organizations") {

            val collegeId =
                call.parameters["id"]?.toLongOrNull()

            if (collegeId == null) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Invalid college ID"
                )
                return@get
            }

            val collegeLookupService =
                CollegeLookupService(connection)

            val organizations =
                collegeLookupService.getOrganizations(collegeId)

            call.respond(organizations)
        }

        get("/categories") {

            val categories =
                EventCategoryService(connection).getAllCategories()

            call.respond(categories)
        }
        // Public event discovery

        authenticate("auth-jwt", optional = true) {
        get("/events") {
            val params = call.request.queryParameters

            val collegeId = params["collegeId"]?.toLongOrNull()
            val categoryId = params["categoryId"]?.toLongOrNull()
            val city = params["city"]
            val from = params["from"]
            val to = params["to"]

            try {
                from?.let { java.time.LocalDate.parse(it) }
                to?.let { java.time.LocalDate.parse(it) }
            } catch (e: java.time.format.DateTimeParseException) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "Dates must be in YYYY-MM-DD format"
                )
                return@get
            }

            val nearLat = params["lat"]?.toDoubleOrNull()
            val nearLng = params["lng"]?.toDoubleOrNull()
            val radiusKm = params["radiusKm"]?.toDoubleOrNull()

            if ((params["lat"] != null || params["lng"] != null) &&
                (nearLat == null || nearLng == null ||
                    nearLat !in -90.0..90.0 || nearLng !in -180.0..180.0)
            ) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "lat and lng must both be valid coordinates"
                )
                return@get
            }

            if (params["radiusKm"] != null &&
                (radiusKm == null || radiusKm <= 0.0 || radiusKm > 20000.0)
            ) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    "radiusKm must be a number between 0 and 20000"
                )
                return@get
            }

            val viewerId = call.principal<JWTPrincipal>()
                ?.payload?.subject?.toLongOrNull()
            val viewerCollegeId = viewerId?.let { UserService(connection).getCollegeId(it) }

            val events = eventService.getAllEvents(
                collegeId, categoryId, city, from, to, viewerCollegeId,
                params["search"], params["sort"],
                nearLat, nearLng, radiusKm
            )
            call.respond(events)
        }
        }
        authenticate("auth-jwt", optional = true) {
        get("/events/{id}") {

            val eventId = call.parameters["id"]?.toLongOrNull()

            if (eventId == null) {
                call.respondText(
                    "Invalid event ID",
                    status = io.ktor.http.HttpStatusCode.BadRequest
                )
                return@get
            }

            val event = eventService.getEventById(eventId)

            val viewerId = call.principal<JWTPrincipal>()
                ?.payload?.subject?.toLongOrNull()
            val viewerCollegeId = viewerId?.let { UserService(connection).getCollegeId(it) }

            val hidden = event != null &&
                !event.acceptVisitors &&
                event.collegeId != viewerCollegeId

            if (event == null || event.status != "APPROVED" || hidden) {
                call.respondText(
                    "Event not found",
                    status = io.ktor.http.HttpStatusCode.NotFound
                )
                return@get
            }

            call.respond(event)
        }
        }

        authenticate("auth-jwt", optional = true) {
            get("/events/{id}/reviews") {

                val eventId = call.parameters["id"]?.toLongOrNull()

                if (eventId == null) {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid event ID")
                    return@get
                }

                val viewerId = call.principal<JWTPrincipal>()
                    ?.payload?.subject?.toLongOrNull()
                val viewerCollegeId = viewerId?.let { UserService(connection).getCollegeId(it) }

                val event = eventService.getEventById(eventId)

                if (event == null ||
                    event.status != "APPROVED" ||
                    (!event.acceptVisitors && event.collegeId != viewerCollegeId)
                ) {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound, "Event not found")
                    return@get
                }

                call.respond(ReviewService(connection).getReviews(eventId))
            }
        }
        
        // Protected event management

        authenticate("auth-jwt") {
            get("/admin/applications/pending") {

    val principal = call.principal<JWTPrincipal>()!!

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@get
    }

    val allowedRoles = setOf(
        "PLATFORM_ADMIN",
        "COLLEGE_ADMIN",
        "FACULTY_COORDINATOR",
        "EVENT_COORDINATOR"
    )

    if (actor.role !in allowedRoles) {
        call.respond(HttpStatusCode.Forbidden, "You do not have permission to review applications")
        return@get
    }

    call.respond(
        adminQueueService.getPendingApplications(actor.role, actor.collegeId)
    )
}

get("/admin/events/pending") {

    val principal = call.principal<JWTPrincipal>()!!

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@get
    }

    val allowedRoles = setOf(
        "PLATFORM_ADMIN",
        "COLLEGE_ADMIN",
        "FACULTY_COORDINATOR",
        "EVENT_COORDINATOR"
    )

    if (actor.role !in allowedRoles) {
        call.respond(HttpStatusCode.Forbidden, "You do not have permission to view pending events")
        return@get
    }

    // Platform Admin may filter by college; everyone else sees only their own college
    val collegeId = if (actor.role == "PLATFORM_ADMIN") {
        call.request.queryParameters["collegeId"]?.toLongOrNull()
    } else {
        actor.collegeId
    }

    if (actor.role != "PLATFORM_ADMIN" && collegeId == null) {
        call.respond(emptyList<PendingEvent>())
        return@get
    }

    val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
    val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

    call.respond(
        adminQueueService.getPendingEvents(collegeId, limit, (page - 1) * limit)
    )
}
            get("/admin/users") {

    val principal = call.principal<JWTPrincipal>()!!

    if (!principal.hasAnyRole("PLATFORM_ADMIN", "COLLEGE_ADMIN")) {
        call.respond(HttpStatusCode.Forbidden, "You do not have permission to view users")
        return@get
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@get
    }

    // College Admins are always limited to their own college
    val collegeId = if (actor.role == "COLLEGE_ADMIN") {
        actor.collegeId
    } else {
        call.request.queryParameters["collegeId"]?.toLongOrNull()
    }

    val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
    val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

    val users = adminService.listUsers(
        collegeId = collegeId,
        role = call.request.queryParameters["role"],
        status = call.request.queryParameters["status"],
        search = call.request.queryParameters["search"]?.takeIf { it.isNotBlank() },
        limit = limit,
        offset = (page - 1) * limit
    )

    call.respond(users)
}

delete("/admin/users/{id}") {

    val principal = call.principal<JWTPrincipal>()!!

    if (!principal.hasAnyRole("PLATFORM_ADMIN", "COLLEGE_ADMIN")) {
        call.respond(HttpStatusCode.Forbidden, "You do not have permission to delete users")
        return@delete
    }

    val targetId = call.parameters["id"]?.toLongOrNull()

    if (targetId == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
        return@delete
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@delete
    }

    val target = adminService.getUserById(targetId)

    if (target == null) {
        call.respond(HttpStatusCode.NotFound, "User not found")
        return@delete
    }

    if (target.accountStatus == "DELETED") {
        call.respond(HttpStatusCode.Conflict, "User is already deleted")
        return@delete
    }

    if (!canManage(actor, target)) {
        call.respond(HttpStatusCode.Forbidden, "You cannot manage this user")
        return@delete
    }

    adminService.softDeleteUser(targetId)

    call.respondText("User deleted successfully")
}

for ((action, newStatus) in listOf("suspend" to "SUSPENDED", "reactivate" to "ACTIVE")) {

    patch("/admin/users/{id}/$action") {

        val principal = call.principal<JWTPrincipal>()!!

        if (!principal.hasAnyRole("PLATFORM_ADMIN", "COLLEGE_ADMIN")) {
            call.respond(HttpStatusCode.Forbidden, "You do not have permission to manage users")
            return@patch
        }

        val targetId = call.parameters["id"]?.toLongOrNull()

        if (targetId == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
            return@patch
        }

        val actor = adminService.getUserById(principal.payload.subject.toLong())

        if (actor == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
            return@patch
        }

        val target = adminService.getUserById(targetId)

        if (target == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return@patch
        }

        if (!canManage(actor, target)) {
            call.respond(HttpStatusCode.Forbidden, "You cannot manage this user")
            return@patch
        }

        adminService.setAccountStatus(targetId, newStatus)

        call.respondText("Account status set to $newStatus")
    }
}

post("/colleges/{id}/departments") {

    val principal = call.principal<JWTPrincipal>()!!
    val collegeId = call.parameters["id"]?.toLongOrNull()

    if (collegeId == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid college ID")
        return@post
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@post
    }

    if (!canManageCollege(actor, collegeId)) {
        call.respond(HttpStatusCode.Forbidden, "You cannot manage departments of this college")
        return@post
    }

    val request = call.receive<CreateDepartmentRequest>()
    val name = request.departmentName.trim()
    val code = request.departmentCode?.trim()?.ifBlank { null }

    if (name.isEmpty() || name.length > 100) {
        call.respond(HttpStatusCode.BadRequest, "Department name is required (max 100 characters)")
        return@post
    }

    if (code != null && code.length > 20) {
        call.respond(HttpStatusCode.BadRequest, "Department code must be 20 characters or fewer")
        return@post
    }

    try {
        call.respond(
            HttpStatusCode.Created,
            collegeManagementService.addDepartment(collegeId, name, code)
        )
    } catch (e: SQLException) {
        when (e.sqlState) {
            "23505" -> call.respond(HttpStatusCode.Conflict, "This department already exists in the college")
            "23503" -> call.respond(HttpStatusCode.NotFound, "College not found")
            else -> throw e
        }
    }
}

delete("/departments/{id}") {

    val principal = call.principal<JWTPrincipal>()!!
    val departmentId = call.parameters["id"]?.toLongOrNull()

    if (departmentId == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid department ID")
        return@delete
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@delete
    }

    val collegeId = collegeManagementService.getDepartmentCollegeId(departmentId)

    if (collegeId == null) {
        call.respond(HttpStatusCode.NotFound, "Department not found")
        return@delete
    }

    if (!canManageCollege(actor, collegeId)) {
        call.respond(HttpStatusCode.Forbidden, "You cannot manage departments of this college")
        return@delete
    }

    try {
        collegeManagementService.deleteDepartment(departmentId)
        call.respondText("Department deleted successfully")
    } catch (e: SQLException) {
        if (e.sqlState == "23503") {
            call.respond(
                HttpStatusCode.Conflict,
                "This department is still used by users or applications and cannot be deleted"
            )
        } else throw e
    }
}

post("/colleges/{id}/organizations") {

    val principal = call.principal<JWTPrincipal>()!!
    val collegeId = call.parameters["id"]?.toLongOrNull()

    if (collegeId == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid college ID")
        return@post
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@post
    }

    if (!canManageCollege(actor, collegeId)) {
        call.respond(HttpStatusCode.Forbidden, "You cannot manage organizations of this college")
        return@post
    }

    val request = call.receive<CreateOrganizationRequest>()
    val name = request.organizationName.trim()
    val type = request.organizationType.trim().uppercase()

    if (name.isEmpty() || name.length > 100) {
        call.respond(HttpStatusCode.BadRequest, "Organization name is required (max 100 characters)")
        return@post
    }

    if (type.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, "Organization type is required")
        return@post
    }

    try {
        call.respond(
            HttpStatusCode.Created,
            collegeManagementService.addOrganization(collegeId, name, type)
        )
    } catch (e: SQLException) {
        when (e.sqlState) {
            "23505" -> call.respond(HttpStatusCode.Conflict, "This organization already exists in the college")
            "23503" -> call.respond(HttpStatusCode.NotFound, "College not found")
            "23514" -> call.respond(HttpStatusCode.BadRequest, "Invalid organization type")
            else -> throw e
        }
    }
}

delete("/organizations/{id}") {

    val principal = call.principal<JWTPrincipal>()!!
    val organizationId = call.parameters["id"]?.toLongOrNull()

    if (organizationId == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid organization ID")
        return@delete
    }

    val actor = adminService.getUserById(principal.payload.subject.toLong())

    if (actor == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
        return@delete
    }

    val collegeId = collegeManagementService.getOrganizationCollegeId(organizationId)

    if (collegeId == null) {
        call.respond(HttpStatusCode.NotFound, "Organization not found")
        return@delete
    }

    if (!canManageCollege(actor, collegeId)) {
        call.respond(HttpStatusCode.Forbidden, "You cannot manage organizations of this college")
        return@delete
    }

    try {
        collegeManagementService.deleteOrganization(organizationId)
        call.respondText("Organization deleted successfully")
    } catch (e: SQLException) {
        if (e.sqlState == "23503") {
            call.respond(
                HttpStatusCode.Conflict,
                "This organization is still used by users or applications and cannot be deleted"
            )
        } else throw e
    }
}
            post("/events") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@post
                }

                if (!principal.hasAnyRole(
                        "STUDENT_ORGANIZER",
                        "EVENT_COORDINATOR",
                        "FACULTY_COORDINATOR",
                        "COLLEGE_ADMIN",
                        "PLATFORM_ADMIN"
                    )
                ) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You do not have permission to create events"
                    )
                    return@post
                }

                val request = call.receive<CreateEventRequest>()

                val userId = principal.payload.subject.toLongOrNull()

                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "Invalid authentication token"
                    )
                    return@post
                }

                val collegeId =
                    if (principal.hasRole("PLATFORM_ADMIN")) {
                        request.collegeId
                    } else {
                        UserService(connection).getCollegeId(userId)
                    }

                if (collegeId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Your account is not linked to a college"
                    )
                    return@post
                }

                val event = eventService.createEvent(
                    request.copy(createdBy = userId, collegeId = collegeId)
                )

                call.respond(
                    io.ktor.http.HttpStatusCode.Created,
                    event
                )
            }

            put("/events/{id}") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@put
                }

                if (!principal.hasAnyRole(
                        "STUDENT_ORGANIZER",
                        "EVENT_COORDINATOR",
                        "FACULTY_COORDINATOR",
                        "COLLEGE_ADMIN",
                        "PLATFORM_ADMIN"
                    )
                ) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You do not have permission to update events"
                    )
                    return@put
                }

                val eventId = call.parameters["id"]?.toLongOrNull()

                if (eventId == null) {
                    call.respondText(
                        "Invalid event ID",
                        status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    return@put
                }

                val request = call.receive<CreateEventRequest>()

                                val userId = principal.payload.subject.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
                    return@put
                }

                val existing = eventService.getEventById(eventId)

                if (existing == null) {
                    call.respondText("Event not found", status = HttpStatusCode.NotFound)
                    return@put
                }

                if (!canManageEvent(principal, userId, existing, UserService(connection))) {
                    call.respond(HttpStatusCode.Forbidden, "You can only edit your own events")
                    return@put
                }

                val event = eventService.updateEvent(
                    eventId,
                    request.copy(
                        collegeId = existing.collegeId,
                        createdBy = existing.createdBy
                    )
                )

                if (event == null) {
                    call.respondText(
                        "Event not found",
                        status = io.ktor.http.HttpStatusCode.NotFound
                    )
                    return@put
                }

                call.respond(event)
            }

            patch("/events/{id}") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@patch
                }

                if (!principal.hasAnyRole(
                        "STUDENT_ORGANIZER",
                        "EVENT_COORDINATOR",
                        "FACULTY_COORDINATOR",
                        "COLLEGE_ADMIN",
                        "PLATFORM_ADMIN"
                    )
                ) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You do not have permission to update events"
                    )
                    return@patch
                }

                val eventId = call.parameters["id"]?.toLongOrNull()

                if (eventId == null) {
                    call.respondText(
                        "Invalid event ID",
                        status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    return@patch
                }

                val request = call.receive<PatchEventRequest>()

                                val userId = principal.payload.subject.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
                    return@patch
                }

                val existing = eventService.getEventById(eventId)

                if (existing == null) {
                    call.respondText("Event not found", status = HttpStatusCode.NotFound)
                    return@patch
                }

                if (!canManageEvent(principal, userId, existing, UserService(connection))) {
                    call.respond(HttpStatusCode.Forbidden, "You can only edit your own events")
                    return@patch
                }

                val event = eventService.patchEvent(
                    eventId,
                    request.copy(collegeId = null, createdBy = null)
                )

                if (event == null) {
                    call.respondText(
                        "Event not found",
                        status = io.ktor.http.HttpStatusCode.NotFound
                    )
                    return@patch
                }

                call.respond(event)
            }

            delete("/events/{id}") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@delete
                }

                if (!principal.hasAnyRole(
                        "COLLEGE_ADMIN",
                        "PLATFORM_ADMIN"
                    )
                ) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You do not have permission to delete events"
                    )
                    return@delete
                }

                val eventId = call.parameters["id"]?.toLongOrNull()

                if (eventId == null) {
                    call.respondText(
                        "Invalid event ID",
                        status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                                val userId = principal.payload.subject.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid authentication token")
                    return@delete
                }

                val existing = eventService.getEventById(eventId)

                if (existing == null) {
                    call.respondText("Event not found", status = HttpStatusCode.NotFound)
                    return@delete
                }

                if (!canManageEvent(principal, userId, existing, UserService(connection))) {
                    call.respond(HttpStatusCode.Forbidden, "You can only delete events of your own college")
                    return@delete
                }

                val deleted = eventService.deleteEvent(eventId)

                if (!deleted) {
                    call.respondText(
                        "Event not found",
                        status = io.ktor.http.HttpStatusCode.NotFound
                    )
                    return@delete
                }

                call.respondText(
                    "Event deleted successfully",
                    status = io.ktor.http.HttpStatusCode.OK
                )
            }

            post("/events/{id}/verify") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@post
                }

                val userId = principal.payload.subject.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (userId == null || eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                if (!principal.hasAnyRole("COLLEGE_ADMIN", "PLATFORM_ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, "Only a college admin can verify events")
                    return@post
                }

                val event = eventService.getEventById(eventId)

                if (event == null) {
                    call.respond(HttpStatusCode.NotFound, "Event not found")
                    return@post
                }

                if (!principal.hasRole("PLATFORM_ADMIN") &&
                    UserService(connection).getCollegeId(userId) != event.collegeId
                ) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "You can only verify events of your own college"
                    )
                    return@post
                }

                if (event.status != "APPROVED") {
                    call.respond(HttpStatusCode.Conflict, "Only approved events can be verified")
                    return@post
                }

                if (event.verificationStatus == "VERIFIED") {
                    call.respond(HttpStatusCode.Conflict, "Event is already verified")
                    return@post
                }

                eventService.markVerified(eventId)

                call.respond(HttpStatusCode.OK, "Event verified")
            }

            post("/events/{id}/review") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@post
                }

                val reviewerId = principal.payload.subject.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (reviewerId == null || eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                if (!principal.hasAnyRole(
                        "FACULTY_COORDINATOR",
                        "EVENT_COORDINATOR",
                        "COLLEGE_ADMIN",
                        "PLATFORM_ADMIN"
                    )
                ) {
                    call.respond(HttpStatusCode.Forbidden, "You do not have permission to review events")
                    return@post
                }

                val request = call.receive<EventReviewRequest>()

                if (request.status != "APPROVED" && request.status != "REJECTED") {
                    call.respond(HttpStatusCode.BadRequest, "Status must be APPROVED or REJECTED")
                    return@post
                }

                if (request.status == "REJECTED" && request.comments.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "A reason is required when rejecting an event")
                    return@post
                }

                val event = eventService.getEventById(eventId)

                if (event == null) {
                    call.respond(HttpStatusCode.NotFound, "Event not found")
                    return@post
                }

                if (event.status != "PENDING") {
                    call.respond(HttpStatusCode.Conflict, "Only pending events can be reviewed")
                    return@post
                }

                val userService = UserService(connection)

                if (!principal.hasRole("PLATFORM_ADMIN")) {

                    if (event.createdBy == reviewerId) {
                        call.respond(HttpStatusCode.Forbidden, "You cannot review your own event")
                        return@post
                    }

                    if (userService.getCollegeId(reviewerId) != event.collegeId) {
                        call.respond(HttpStatusCode.Forbidden, "You can only review events of your own college")
                        return@post
                    }

                    val creatorRole = event.createdBy?.let { userService.getRole(it) }

                    val allowed = when (creatorRole) {
                        "STUDENT_ORGANIZER" -> principal.hasAnyRole(
                            "FACULTY_COORDINATOR",
                            "EVENT_COORDINATOR",
                            "COLLEGE_ADMIN"
                        )
                        "FACULTY_COORDINATOR", "EVENT_COORDINATOR" -> principal.hasRole("COLLEGE_ADMIN")
                        else -> false
                    }

                    if (!allowed) {
                        call.respond(HttpStatusCode.Forbidden, "You are not authorized to review this event")
                        return@post
                    }
                }

                val updated = eventService.reviewEvent(
                    eventId,
                    request.status,
                    reviewerId,
                    request.comments
                )

                if (!updated) {
                    call.respond(HttpStatusCode.Conflict, "Event could not be reviewed")
                    return@post
                }

                call.respond(HttpStatusCode.OK, "Event ${request.status.lowercase()}")
            }

            post("/events/{id}/submit") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@post
                }

                val userId = principal.payload.subject.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (userId == null || eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                val event = eventService.getEventById(eventId)

                if (event == null) {
                    call.respond(HttpStatusCode.NotFound, "Event not found")
                    return@post
                }

                if (event.createdBy != userId && !principal.hasRole("PLATFORM_ADMIN")) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Only the event creator can submit this event"
                    )
                    return@post
                }

                if (event.status != "DRAFT" && event.status != "REJECTED") {
                    call.respond(
                        HttpStatusCode.Conflict,
                        "Only draft or rejected events can be submitted"
                    )
                    return@post
                }

                eventService.updateStatus(eventId, "PENDING")

                call.respond(HttpStatusCode.OK, "Event submitted for approval")
            }

            get("/users/me/events") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@get
                }

                call.respond(eventService.getEventsByCreator(userId))
            }

            post("/events/{id}/save") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@post
                }

                if (eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
                    return@post
                }

                val event = eventService.getEventById(eventId)
                val userCollegeId = UserService(connection).getCollegeId(userId)

                if (event == null ||
                    event.status != "APPROVED" ||
                    (!event.acceptVisitors && event.collegeId != userCollegeId)
                ) {
                    call.respond(HttpStatusCode.NotFound, "Event not found")
                    return@post
                }

                SavedEventService(connection).saveEvent(userId, eventId)

                call.respond(HttpStatusCode.OK, "Event saved")
            }

            delete("/events/{id}/save") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@delete
                }

                if (eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
                    return@delete
                }

                val removed = SavedEventService(connection).unsaveEvent(userId, eventId)

                if (!removed) {
                    call.respond(HttpStatusCode.NotFound, "This event was not in your saved list")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Event removed from saved events")
            }

            get("/users/me/saved") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@get
                }

                val viewerCollegeId = UserService(connection).getCollegeId(userId)

                call.respond(SavedEventService(connection).getSavedEvents(userId, viewerCollegeId))
            }

                        post("/events/{id}/reviews") {

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject?.toLongOrNull()
                val eventId = call.parameters["id"]?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    return@post
                }

                if (eventId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid event ID")
                    return@post
                }

                val request = call.receive<ReviewRequest>()

                if (request.rating !in 1..5) {
                    call.respond(HttpStatusCode.BadRequest, "Rating must be between 1 and 5")
                    return@post
                }

                val reviewText = request.reviewText?.trim()?.ifBlank { null }

                if (reviewText != null && reviewText.length > 1000) {
                    call.respond(HttpStatusCode.BadRequest, "Review text must be 1000 characters or fewer")
                    return@post
                }

                val event = eventService.getEventById(eventId)
                val userCollegeId = UserService(connection).getCollegeId(userId)

                if (event == null ||
                    event.status != "APPROVED" ||
                    (!event.acceptVisitors && event.collegeId != userCollegeId)
                ) {
                    call.respond(HttpStatusCode.NotFound, "Event not found")
                    return@post
                }

                if (event.createdBy == userId) {
                    call.respond(HttpStatusCode.Forbidden, "You cannot review your own event")
                    return@post
                }

                val reviewService = ReviewService(connection)
                val reviewState = reviewService.getReviewState(eventId)

                if (reviewState == "NOT_ENDED") {
                    call.respond(HttpStatusCode.Conflict, "Reviews open after the event ends")
                    return@post
                }

                if (reviewState != "OPEN") {
                    call.respond(HttpStatusCode.Conflict, "The review period for this event has ended")
                    return@post
                }

                if (!reviewService.addReview(eventId, userId, request.rating, reviewText)) {
                    call.respond(HttpStatusCode.Conflict, "You have already reviewed this event")
                    return@post
                }

                call.respond(HttpStatusCode.Created, "Review submitted")
            }

            get("/auth/me") {

                val principal = call.principal<JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@get
                }

                val userId = principal.payload.subject.toLongOrNull()

                if (userId == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Invalid authentication token"
                    )
                    return@get
                }

                val userProfileService =
                    UserProfileService(connection)

                val profile =
                    userProfileService.getUserProfile(userId)

                if (profile == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        "User profile not found"
                    )
                    return@get
                }

                val departments =
                    userProfileService.getDepartments(userId)

                val organizations =
                    userProfileService.getOrganizations(userId)

                call.respond(
                    AuthMeResponse(
                        profile = profile,
                        departments = departments,
                        organizations = organizations
                    )
                )
            } 
            post("/auth/applications/review") {

                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()

                if (principal == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@post
                }

                val reviewerUserId =
                    principal.payload.subject.toLongOrNull()

                if (reviewerUserId == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Invalid authentication token"
                    )
                    return@post
                }

                val reviewerRole =
                    principal.payload
                        .getClaim("role")
                        .asString()

                val allowedReviewerRoles = setOf(
                    "PLATFORM_ADMIN",
                    "COLLEGE_ADMIN",
                    "EVENT_COORDINATOR",
                    "FACULTY_COORDINATOR"
                )

                if (reviewerRole !in allowedReviewerRoles) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You do not have permission to review applications"
                    )
                    return@post
                }

                val request =
                    call.receive<RoleApplicationReviewRequest>()

                val validationError =
                    validateRoleApplicationReview(request)

                if (validationError != null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        validationError
                    )
                    return@post
                }

                val roleApplicationService =
                    RoleApplicationService(connection)

                if (!roleApplicationService.applicationExists(request.applicationId)) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        "Application not found"
                    )
                    return@post
                }

                val applicationRole =
                    roleApplicationService.getApplicationRole(
                        request.applicationId
                    )

                if (applicationRole == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        "Application not found"
                    )
                    return@post
                }

                /*
                * Determine who can review this application.
                */

                val allowedForApplication = when (applicationRole) {

                    "STUDENT_ORGANIZER" ->
                        reviewerRole == "FACULTY_COORDINATOR" ||
                        reviewerRole == "EVENT_COORDINATOR"

                    "FACULTY_COORDINATOR",
                    "EVENT_COORDINATOR" ->
                        reviewerRole == "COLLEGE_ADMIN"

                    "COLLEGE_ADMIN" ->
                        reviewerRole == "PLATFORM_ADMIN"

                    else ->
                        false
                }

                if (!allowedForApplication) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        "You are not authorized to review this application"
                    )
                    return@post
                }

                roleApplicationService.addReview(
                    applicationId = request.applicationId,
                    reviewerUserId = reviewerUserId,
                    reviewerRole = reviewerRole,
                    status = request.status,
                    comments = request.comments
                )

                if (request.status == "REJECTED") {

                    roleApplicationService.rejectApplication(
                        applicationId = request.applicationId,
                        reason = request.comments!!
                    )

                    call.respond(
                        io.ktor.http.HttpStatusCode.OK,
                        "Application rejected successfully"
                    )

                    return@post
                }

                try {
                    val userId = roleApplicationService.approveApplication(
                        request.applicationId
                    )

                    call.respond(
                        io.ktor.http.HttpStatusCode.OK,
                        "Application approved successfully. User ID: $userId"
                    )
                } catch (exception: IllegalStateException) {

                    call.respond(
                        io.ktor.http.HttpStatusCode.Conflict,
                        exception.message ?: "Application could not be approved"
                    )
                }
            }
        }
    }
}