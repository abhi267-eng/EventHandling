package com.eventhandling

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
        // Public event discovery

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

            val events = eventService.getAllEvents(collegeId, categoryId, city, from, to)
            call.respond(events)
        }

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

            if (event == null || event.status != "APPROVED") {
                call.respondText(
                    "Event not found",
                    status = io.ktor.http.HttpStatusCode.NotFound
                )
                return@get
            }

            call.respond(event)
        }

        // Protected event management

        authenticate("auth-jwt") {

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