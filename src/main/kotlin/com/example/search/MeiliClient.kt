package com.example.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MeiliClient")

class MeiliClient(private val http: HttpClient, private val baseUrl: String) {

    suspend fun search(index: String, params: SearchParams): SearchResult {
        val body = buildMap<String, Any?> {
            put("q", params.q)
            put("page", params.page)
            put("hitsPerPage", params.hitsPerPage)
            if (params.filter != null) put("filter", params.filter)
            if (params.sort.isNotEmpty()) put("sort", params.sort)
            if (params.facets.isNotEmpty()) put("facets", params.facets)
        }

        val response = http.post("$baseUrl/indexes/$index/search") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            logger.warn("MeiliSearch search error on index={} status={}: {}", index, response.status.value, error)
            throw MeiliException(response.status.value, error)
        }

        @Suppress("UNCHECKED_CAST")
        val raw = response.body<Map<String, Any?>>()
        return raw.toSearchResult()
    }

    suspend fun getDocument(index: String, id: String): Map<String, Any?> {
        val response = http.get("$baseUrl/indexes/$index/documents/$id")

        if (response.status == HttpStatusCode.NotFound) return emptyMap()
        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            logger.warn("MeiliSearch getDocument error on index={} id={} status={}: {}", index, id, response.status.value, error)
            throw MeiliException(response.status.value, error)
        }

        @Suppress("UNCHECKED_CAST")
        return response.body<Map<String, Any?>>()
    }

    suspend fun suggest(index: String, query: String, limit: Int): List<Map<String, Any?>> {
        val body = mapOf("q" to query, "page" to 1, "hitsPerPage" to limit)

        val response = http.post("$baseUrl/indexes/$index/search") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            logger.warn("MeiliSearch suggest error on index={} status={}: {}", index, response.status.value, error)
            throw MeiliException(response.status.value, error)
        }

        @Suppress("UNCHECKED_CAST")
        val raw = response.body<Map<String, Any?>>()
        @Suppress("UNCHECKED_CAST")
        return (raw["hits"] as? List<Map<String, Any?>>) ?: emptyList()
    }

}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toSearchResult(): SearchResult {
    val hits = (this["hits"] as? List<Map<String, Any?>>) ?: emptyList()
    val facetDistribution = this["facetDistribution"] as? Map<String, Map<String, Int>>
    return SearchResult(
        hits = hits,
        query = this["query"] as? String ?: "",
        processingTimeMs = (this["processingTimeMs"] as? Number)?.toLong() ?: 0,
        totalHits = (this["totalHits"] as? Number)?.toInt(),
        page = (this["page"] as? Number)?.toInt(),
        hitsPerPage = (this["hitsPerPage"] as? Number)?.toInt(),
        totalPages = (this["totalPages"] as? Number)?.toInt(),
        facetDistribution = facetDistribution
    )
}

class MeiliException(val statusCode: Int, message: String) : RuntimeException(message)

fun buildMeiliHttpClient(apiKey: String): HttpClient {
    val mapper = ObjectMapper().registerKotlinModule()
    val bearer = "Bearer " + apiKey
    return HttpClient(CIO) {
        install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(mapper)) }
        install(DefaultRequest) { header("Authorization", bearer) }
        expectSuccess = false
    }
}
