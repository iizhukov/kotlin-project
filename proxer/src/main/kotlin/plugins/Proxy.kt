@file:Suppress("ktlint:standard:no-wildcard-imports")

package plugins

import config.Config
import config.ProxyConfig
import features.proxy.LoadBalancerFactory
import features.proxy.RoundRobinLoadBalancer
import features.stats.StatsCollector
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject

fun Application.configureProxy() {
    val config by inject<Config>()
    val loadBalancerFactory by inject<LoadBalancerFactory>()

    routing {
        for (proxy in config.proxy) {
            val loadBalancer = loadBalancerFactory.create(proxy.ips)

            route(proxy.prefix) {
                handle {
                    withContext(Dispatchers.IO) {
                        call.callHandler(proxy, loadBalancer)
                    }
                }

                route("{path...}") {
                    handle {
                        withContext(Dispatchers.IO) {
                            call.callHandler(proxy, loadBalancer)
                        }
                    }
                }

                route("{path...}/") {
                    handle {
                        withContext(Dispatchers.IO) {
                            call.callHandler(proxy, loadBalancer)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun ApplicationCall.callHandler(
    proxy: ProxyConfig,
    loadBalancer: RoundRobinLoadBalancer,
) {
    val config by inject<Config>()
    val client by inject<HttpClient>()
    val redis by inject<RedisCoroutinesCommands<String, String>>()
    val statsCollector by inject<StatsCollector>()

    val path = request.uri.removePrefix(proxy.prefix)
    val endpoint = request.uri.substringBefore('?')

    try {
        statsCollector.trackRequest(endpoint)

        val cacheKey = "cache:${request.uri}"
        val cacheEnabled = proxy.cache && config.cache.enabled

        if (cacheEnabled && request.httpMethod == HttpMethod.Get) {
            val cached = redis.get(cacheKey)
            if (!cached.isNullOrEmpty()) {
                respondText(cached, ContentType.Application.Json)

                statsCollector.trackCached(endpoint)
                return
            }
        }

        val serverIp = loadBalancer.nextServer()
        val url = "${proxy.protocol}://$serverIp${proxy.target}$path"

        val proxyResponse =
            client.request(url) {
                method = request.httpMethod

                request.headers.forEach { key, values ->
                    headers.appendAll(key, values)
                }

                if (request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                    setBody(request.receiveChannel())
                }
            }

        val responseBytes = proxyResponse.readBytes()

        if (cacheEnabled && request.httpMethod == HttpMethod.Get) {
            val duration = config.cache.duration.toLong()

            if (duration != -1L) {
                redis.setex(cacheKey, duration, responseBytes.decodeToString())
            } else {
                redis.set(cacheKey, responseBytes.decodeToString())
            }
        }

        respond(
            object : OutgoingContent.ByteArrayContent() {
                override val contentType: ContentType = proxyResponse.contentType() ?: ContentType.Application.Json
                override val status: HttpStatusCode = proxyResponse.status
                override val headers: Headers = proxyResponse.headers

                override fun bytes(): ByteArray = responseBytes
            },
        )

        statsCollector.trackSuccess(endpoint)
    } catch (e: Exception) {
        statsCollector.trackFailure(endpoint)
        throw e
    }
}
