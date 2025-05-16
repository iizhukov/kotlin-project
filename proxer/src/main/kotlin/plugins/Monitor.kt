@file:Suppress("ktlint:standard:no-wildcard-imports")

package plugins

import features.stats.StatsCollector
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureMonitor() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            },
        )
    }

    val statsCollector by inject<StatsCollector>()

    routing {
        get("/stats") {
            try {
                val stats = statsCollector.getStats()
                call.respond(stats)
            } catch (e: Exception) {
                call.respondText(
                    "Failed to get stats: ${e.message}",
                    status = HttpStatusCode.InternalServerError,
                )
                application.log.error("Stats error", e)
            }
        }

        get("/stats/history") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
            val endpoint = call.request.queryParameters["endpoint"]

//            try {
            val history = statsCollector.getHistory(page, pageSize, endpoint)
            call.respond(history)
//            } catch (e: Exception) {
//                call.respondText(
//                    "Failed to get history: ${e.message}",
//                    status = HttpStatusCode.InternalServerError,
//                )
//                application.log.error("History error", e)
//            }
        }
    }
}
