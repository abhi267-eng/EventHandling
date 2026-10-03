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
            val events = eventService.getAllEvents()
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

            if (event == null) {
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

                val event = eventService.createEvent(request)

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

                val event = eventService.updateEvent(eventId, request)

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

                val event = eventService.patchEvent(eventId, request)

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