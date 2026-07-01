package com.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host, module = Application::module).start(wait = true)
}

fun Application.module() {
    val client = HttpClient(CIO)

    environment.monitor.subscribe(ApplicationStopping) {
        client.close()
    }

    routing {
        get("/api/search") {
            val query = call.request.queryParameters["q"]
                ?: return@get call.respondText(
                    "{\"message\":\"Missing query parameter 'q'\"}",
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )

            val meiliResponse = try {
                client.get("http://127.0.0.1:7700/indexes/movies/search") {
                    parameter("q", query)
                }
            } catch (exception: Exception) {
                this@module.environment.log.warn("Local Meilisearch request failed", exception)
                return@get call.respondText(
                    text = "{\"message\":\"Search service temporarily unavailable\"}",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.ServiceUnavailable
                )
            }

            call.respondText(
                text = meiliResponse.body(),
                contentType = ContentType.Application.Json,
                status = meiliResponse.status
            )
        }
    }
}
