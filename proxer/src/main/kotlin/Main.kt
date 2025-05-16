@file:Suppress("ktlint:standard:no-wildcard-imports")

import config.ConfigLoader
import core.cacheModule
import core.coreModule
import core.statsModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
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
