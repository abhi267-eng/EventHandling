package com.eventhandling

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*

fun Application.configureRouting() {

    val connection = connectToPostgres(embedded = false)
    val eventService = EventService(connection)

    routing {

        get("/") {
            call.respondText("Hello, World!")
        }
        

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
        post("/events") {

            val request = call.receive<CreateEventRequest>()

            val event = eventService.createEvent(request)

            call.respond(
                io.ktor.http.HttpStatusCode.Created,
                event
            )
        }
        put("/events/{id}") {

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
    
    }
}