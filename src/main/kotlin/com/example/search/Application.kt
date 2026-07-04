package com.example.search

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    val config = Config()
    val httpClient = buildMeiliHttpClient(config.meiliApiKey)
    val meiliClient = MeiliClient(httpClient, config.meiliHost)

    if (config.seedOnStartup) {
        seedIndexes(meiliClient)
    }

    embeddedServer(Netty, port = config.port) {
        configureApplication(meiliClient)
    }.start(wait = true)
}

fun Application.configureApplication(client: MeiliClient) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    configureRouting(client)
}
