@file:Suppress("ktlint:standard:no-wildcard-imports")

import config.Config
import config.ConfigLoader
import core.cacheModule
import core.coreModule
import core.statsModule
import features.proxy.LoadBalancerFactory
import features.queue.QueueManager
import features.stats.StatsCollector
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.lettuce.core.RedisClient
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import plugins.*

fun main(): Unit =
    runBlocking {
        val config = ConfigLoader.loadConfig("config.json")

        val mainServer =
            Thread {
                embeddedServer(Netty, host = config.server.host, port = config.server.port) {
                    install(Koin) {
                        modules(
                            coreModule(config),
                        )

                        if (config.cache.enabled) {
                            modules(cacheModule)
                        }

                        if (config.stats.enabled) {
                            modules(statsModule)
                        }
                    }

                    initKoin()

                    configureProxy()
                    configureQueues()
                }.start(wait = true)
            }.apply { start() }

        if (config.stats.enabled) {
            Thread {
                embeddedServer(Netty, host = config.stats.server.host, port = config.stats.server.port) {
                    configureMonitor()
                }.start(wait = true)
            }.start()
        }

        mainServer.join()
    }

fun Application.initKoin() {
    val config by inject<Config>()
    val queueManager by inject<QueueManager>()
    val httpClient by inject<HttpClient>()
    val loadBalancerFactory by inject<LoadBalancerFactory>()

    if (config.cache.enabled) {
        val redisClient by inject<RedisClient>()
    }

    if (config.stats.enabled) {
        val statsCollector by inject<StatsCollector>()
    }
}
