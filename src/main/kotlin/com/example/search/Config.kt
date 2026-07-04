package com.example.search

data class Config(
    val meiliHost: String = System.getenv("MEILI_HOST") ?: "http://meilisearch:7700",
    val meiliApiKey: String = System.getenv("MEILI_MASTER_KEY") ?: "masterKey",
    val port: Int = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080,
    val seedOnStartup: Boolean = System.getenv("SEED_ON_STARTUP")?.toBooleanStrictOrNull() ?: true
)
