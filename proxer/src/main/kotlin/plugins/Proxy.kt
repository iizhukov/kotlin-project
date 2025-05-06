@file:Suppress("ktlint:standard:no-wildcard-imports")

package plugins

import config.Config
import config.ProxyConfig
import features.proxy.LoadBalancerFactory
import features.proxy.RoundRobinLoadBalancer
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

fun Application.configureProxy() {
    val config by inject<Config>()
    val loadBalancerFactory by inject<LoadBalancerFactory>()

    routing {
        for (proxy in config.proxy) {
            val loadBalancer = loadBalancerFactory.create(proxy.ips)

            route(proxy.prefix) {
                handle {
                    call.callHandler(proxy, loadBalancer)
                }

                route("{path...}") {
                    handle {
                        call.callHandler(proxy, loadBalancer)
                    }
                }

                route("{path...}/") {
                    handle {
                        call.callHandler(proxy, loadBalancer)
                    }
                }
            }
        }
    }
}

suspend fun ApplicationCall.callHandler(
    proxy: ProxyConfig,
    loadBalancer: RoundRobinLoadBalancer,
) {
    val client by inject<HttpClient>()

    val serverIp = loadBalancer.nextServer()
    val path = request.uri.replace(proxy.prefix, proxy.target)
    val url = "${proxy.protocol}://$serverIp$path"

    val proxyResponse =
        client.request(url) {
            method = request.httpMethod

            request.headers.forEach { key, values ->
                headers.appendAll(key, values)
            }

            if (request.httpMethod == HttpMethod.Post ||
                request.httpMethod == HttpMethod.Put ||
                request.httpMethod == HttpMethod.Patch
            ) {
                setBody(request.receiveChannel())
            }
        }

    respond(
        object : OutgoingContent.ByteArrayContent() {
            override val contentType: ContentType = proxyResponse.contentType() ?: ContentType.Application.Json
            override val status: HttpStatusCode = proxyResponse.status
            override val headers: Headers = proxyResponse.headers

            override fun bytes(): ByteArray =
                runBlocking {
                    proxyResponse.readBytes()
                }
        },
    )
}
