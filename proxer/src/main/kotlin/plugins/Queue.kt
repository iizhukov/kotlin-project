@file:Suppress("ktlint:standard:no-wildcard-imports")

package plugins

import config.Config
import features.queue.QueueManager
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject

fun Application.configureQueues() {
    val config by inject<Config>()
    val manager by inject<QueueManager>()

    routing {
        config.queues.forEach { queue ->
            post(queue.endpoint) {
                withContext(Dispatchers.IO) {
                    val body = call.receiveText()

                    manager.send(queue.endpoint, body)

                    call.respondText("Message queued")
                }
            }
        }
    }
}
