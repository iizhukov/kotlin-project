@file:Suppress("ktlint:standard:no-wildcard-imports")

package features.queue

import config.QueueConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

data class QueueMessage(
    val body: String,
)

class QueueWorker(
    private val client: HttpClient,
    private val targetUrl: String,
    private val channel: Channel<QueueMessage>,
) {
    fun start() =
        CoroutineScope(Dispatchers.IO).launch {
            for (message in channel) {
                try {
                    client.post(targetUrl) {
                        setBody(message.body)
                    }
                } catch (e: Exception) {
                    println("Failed to deliver message to $targetUrl: ${e.message}")
                }
            }
        }
}

class QueueManager(
    configs: List<QueueConfig>,
    private val client: HttpClient,
) {
    private val workers: Map<String, Channel<QueueMessage>> =
        configs.associate { queue ->
            val url = "${queue.protocol}://${queue.ip}${queue.target}"
            val channel = Channel<QueueMessage>(capacity = Channel.UNLIMITED)
            QueueWorker(client, url, channel).start()
            queue.endpoint to channel
        }

    fun send(
        endpoint: String,
        message: String,
    ) {
        getChannel(endpoint)?.trySend(QueueMessage(body = message))
    }

    private fun getChannel(endpoint: String): Channel<QueueMessage>? = workers[endpoint]
}
