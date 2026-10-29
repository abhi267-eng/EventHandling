package com.eventhandling

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.application.*
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

private fun errorCode(status: HttpStatusCode): String =
    status.description.trim().uppercase().replace(' ', '_')

/*
 * Rewrites every error response (status 400 and above) that is not
 * already JSON into { "error": "...", "message": "..." }.
 * This covers the plain-text errors in Routing.kt, the JWT challenge,
 * and unknown URLs / wrong methods that have no body at all.
 */
val ErrorResponseFormatter = createApplicationPlugin(name = "ErrorResponseFormatter") {

    on(ResponseBodyReadyForSend) { call, content ->

        val status = content.status ?: call.response.status() ?: return@on

        if (status.value < 400) return@on

        if (content.contentType?.match(ContentType.Application.Json) == true) {
            return@on
        }

        val message = (content as? TextContent)
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: status.description

        val json = Json.encodeToString(
            ErrorResponse.serializer(),
            ErrorResponse(errorCode(status), message)
        )

        transformBodyTo(TextContent(json, ContentType.Application.Json, status))
    }
}

fun Application.configureErrors() {

    install(ErrorResponseFormatter)

    install(StatusPages) {

        exception<Throwable> { call, cause ->

            val status = when (cause) {
                is BadRequestException -> HttpStatusCode.BadRequest
                // Missing or unsupported Content-Type, or a body that cannot be read as the expected type
                is ContentTransformationException -> HttpStatusCode.UnsupportedMediaType
                is NotFoundException -> HttpStatusCode.NotFound
                else -> HttpStatusCode.InternalServerError
            }

            // Full details go to the server log only, never to the client
            if (status == HttpStatusCode.InternalServerError) {
                call.application.log.error(
                    "Unhandled exception on ${call.request.httpMethod.value} ${call.request.uri}",
                    cause
                )
            }

            val message = when (status) {
                HttpStatusCode.UnsupportedMediaType ->
                    "Send the request body as JSON with Content-Type: application/json"
                HttpStatusCode.BadRequest ->
                    "The request is invalid or its JSON body is malformed"
                HttpStatusCode.NotFound ->
                    "Resource not found"
                else ->
                    "Something went wrong on the server"
            }

            call.respond(status, ErrorResponse(errorCode(status), message))
        }
    }
}