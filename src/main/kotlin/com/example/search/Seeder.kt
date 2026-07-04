package com.example.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Seeder")
private val mapper = ObjectMapper().registerKotlinModule()

data class IndexDefinition(
    val name: String,
    val primaryKey: String,
    val searchableAttributes: List<String>,
    val filterableAttributes: List<String>,
    val sortableAttributes: List<String>,
    val seedFile: String
)

val INDEX_DEFINITIONS = listOf(
    IndexDefinition(
        name = "movies",
        primaryKey = "id",
        searchableAttributes = listOf("title", "overview", "director"),
        filterableAttributes = listOf("genre", "year"),
        sortableAttributes = listOf("rating", "year"),
        seedFile = "seed/movies.json"
    ),
    IndexDefinition(
        name = "books",
        primaryKey = "id",
        searchableAttributes = listOf("title", "description", "author"),
        filterableAttributes = listOf("genre", "year"),
        sortableAttributes = listOf("year"),
        seedFile = "seed/books.json"
    )
)

fun seedIndexes(client: MeiliClient) = runBlocking {
    for (def in INDEX_DEFINITIONS) {
        try {
            logger.info("Setting up index: {}", def.name)
            client.createIndex(def.name, def.primaryKey)
            client.updateSettings(
                def.name,
                mapOf(
                    "searchableAttributes" to def.searchableAttributes,
                    "filterableAttributes" to def.filterableAttributes,
                    "sortableAttributes" to def.sortableAttributes
                )
            )
            val resource = Seeder::class.java.classLoader.getResourceAsStream(def.seedFile)
            if (resource == null) {
                logger.warn("Seed file not found: {}", def.seedFile)
                continue
            }
            val documents: List<Map<String, Any?>> = mapper.readValue(resource)
            client.ingestDocuments(def.name, documents)
        } catch (e: Exception) {
            logger.error("Failed to seed index {}: {}", def.name, e.message)
        }
    }
}

object Seeder
