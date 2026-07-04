package com.example.search

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SearchRoutes")

fun Application.configureRouting(client: MeiliClient) {
    routing {
        route("/api") {
            get("/search/{index}") {
                val index = call.parameters["index"]!!
                val params = SearchParams(
                    q = call.request.queryParameters["q"] ?: "",
                    filter = call.request.queryParameters["filter"],
                    sort = call.request.queryParameters["sort"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                    page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                    hitsPerPage = call.request.queryParameters["hitsPerPage"]?.toIntOrNull() ?: 20,
                    facets = call.request.queryParameters["facets"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                )
                try {
                    val result = client.search(index, params)
                    call.respond(result)
                } catch (e: MeiliException) {
                    logger.warn("Search failed: {}", e.message)
                    call.respond(HttpStatusCode.fromValue(upstreamStatus(e.statusCode)), mapOf("error" to e.message))
                }
            }

            get("/fetch/{index}/{id}") {
                val index = call.parameters["index"]!!
                val id = call.parameters["id"]!!
                try {
                    val doc = client.getDocument(index, id)
                    if (doc.isEmpty()) call.respond(HttpStatusCode.NotFound, mapOf("error" to "document not found"))
                    else call.respond(doc)
                } catch (e: MeiliException) {
                    logger.warn("Fetch failed: {}", e.message)
                    call.respond(HttpStatusCode.fromValue(upstreamStatus(e.statusCode)), mapOf("error" to e.message))
                }
            }

            get("/suggest/{index}") {
                val index = call.parameters["index"]!!
                val q = call.request.queryParameters["q"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
                try {
                    val hits = client.suggest(index, q, limit)
                    call.respond(mapOf("suggestions" to hits))
                } catch (e: MeiliException) {
                    logger.warn("Suggest failed: {}", e.message)
                    call.respond(HttpStatusCode.fromValue(upstreamStatus(e.statusCode)), mapOf("error" to e.message))
                }
            }
        }
    }
}

private fun upstreamStatus(meiliStatus: Int): Int = when {
    meiliStatus == 404 -> 404
    meiliStatus in 400..499 -> 400
    else -> 502
}
