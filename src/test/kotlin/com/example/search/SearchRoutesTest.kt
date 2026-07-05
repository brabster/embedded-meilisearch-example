package com.example.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.client.engine.mock.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SearchRoutesTest {

    private val mapper = ObjectMapper().registerKotlinModule()

    private fun searchResponse(q: String = "test") = mapper.writeValueAsString(
        mapOf(
            "hits" to listOf(mapOf("id" to 1, "title" to "Test Movie")),
            "query" to q,
            "processingTimeMs" to 5,
            "totalHits" to 1,
            "page" to 1,
            "hitsPerPage" to 20,
            "totalPages" to 1
        )
    )

    private fun documentResponse() = mapper.writeValueAsString(
        mapOf("id" to 42, "title" to "Found Movie", "genre" to "Drama")
    )

    @Test
    fun `search returns results from MeiliSearch`() = testApplication {
        val mockEngine = MockEngine { request ->
            respond(
                content = searchResponse("inception"),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val http = io.ktor.client.HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            expectSuccess = false
        }
        val meili = MeiliClient(http, "http://mock-meili")

        application { configureApplication(meili) }

        val client = createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
        }

        val response = client.get("/api/search/movies?q=inception")
        assertEquals(HttpStatusCode.OK, response.status)

        @Suppress("UNCHECKED_CAST")
        val body = response.body<Map<String, Any?>>()
        assertNotNull(body["hits"])
    }

    @Test
    fun `fetch returns document by id`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content = documentResponse(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val http = io.ktor.client.HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            expectSuccess = false
        }
        val meili = MeiliClient(http, "http://mock-meili")

        application { configureApplication(meili) }

        val client = createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
        }

        val response = client.get("/api/fetch/movies/42")
        assertEquals(HttpStatusCode.OK, response.status)

        @Suppress("UNCHECKED_CAST")
        val body = response.body<Map<String, Any?>>()
        assertEquals(42, body["id"])
    }

    @Test
    fun `fetch returns 404 when document not found`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"message":"Document `99` not found.","code":"document_not_found","type":"invalid_request","link":""}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val http = io.ktor.client.HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            expectSuccess = false
        }
        val meili = MeiliClient(http, "http://mock-meili")

        application { configureApplication(meili) }

        val response = createClient {}.get("/api/fetch/movies/99")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `suggest returns top hits`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content = searchResponse("inter"),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val http = io.ktor.client.HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            expectSuccess = false
        }
        val meili = MeiliClient(http, "http://mock-meili")

        application { configureApplication(meili) }

        val client = createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
        }

        val response = client.get("/api/suggest/movies?q=inter&limit=5")
        assertEquals(HttpStatusCode.OK, response.status)

        @Suppress("UNCHECKED_CAST")
        val body = response.body<Map<String, Any?>>()
        assertNotNull(body["suggestions"])
    }

    @Test
    fun `search propagates MeiliSearch errors as 502`() = testApplication {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"message":"Internal error"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        val http = io.ktor.client.HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            expectSuccess = false
        }
        val meili = MeiliClient(http, "http://mock-meili")

        application { configureApplication(meili) }

        val response = createClient {}.get("/api/search/movies?q=test")
        assertEquals(HttpStatusCode.BadGateway, response.status)
    }
}
