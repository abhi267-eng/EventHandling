package com.eventhandling

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorsTest {

    /*
     * Builds a tiny in-memory app with the real error handling and a few
     * test routes. No database and no application.yaml modules are used.
     */
    private fun ApplicationTestBuilder.setUp() {

        environment {
            config = MapApplicationConfig()
        }

        application {
            configureSerialization()
            configureErrors()

            routing {
                get("/ok") {
                    call.respondText("fine")
                }

                get("/forbidden") {
                    call.respond(HttpStatusCode.Forbidden, "No access")
                }

                get("/crash") {
                    throw IllegalStateException("secret internal detail")
                }

                post("/login") {
                    call.receive<LoginRequest>()
                    call.respondText("received")
                }
            }
        }
    }

    @Test
    fun `successful responses are not changed`() = testApplication {
        setUp()

        val response = client.get("/ok")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("fine", response.bodyAsText())
    }

    @Test
    fun `plain text errors become the standard json shape`() = testApplication {
        setUp()

        val response = client.get("/forbidden")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(body.contains("\"error\":\"FORBIDDEN\""), body)
        assertTrue(body.contains("\"message\":\"No access\""), body)
    }

    @Test
    fun `unknown url returns 404 in the standard shape`() = testApplication {
        setUp()

        val response = client.get("/nothing-here")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(body.contains("\"error\":\"NOT_FOUND\""), body)
    }

    @Test
    fun `wrong method returns 405 in the standard shape`() = testApplication {
        setUp()

        val response = client.post("/ok")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.MethodNotAllowed, response.status)
        assertTrue(body.contains("\"error\":\"METHOD_NOT_ALLOWED\""), body)
    }

    @Test
    fun `unexpected exceptions return a generic 500 without internal details`() = testApplication {
        setUp()

        val response = client.get("/crash")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(body.contains("\"error\":\"INTERNAL_SERVER_ERROR\""), body)
        assertFalse(body.contains("secret internal detail"), body)
    }

    @Test
    fun `request without a json body returns 415`() = testApplication {
        setUp()

        val response = client.post("/login")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertTrue(body.contains("\"error\":\"UNSUPPORTED_MEDIA_TYPE\""), body)
    }

    @Test
    fun `malformed json returns 400`() = testApplication {
        setUp()

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("{ \"email\": ")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(body.contains("\"error\":\"BAD_REQUEST\""), body)
    }
}