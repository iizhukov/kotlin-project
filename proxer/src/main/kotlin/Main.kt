import config.ConfigLoader
import core.coreModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.koin.ktor.plugin.Koin
import plugins.*

fun main(): Unit =
    runBlocking {
        val config = ConfigLoader.loadConfig("config.json")

        embeddedServer(Netty, port = config.server.port, host = config.server.host) {
            install(Koin) {
                modules(coreModule(config))
            }

            configureProxy()
//            configureQueues()
//            configureMonitoring()
        }.start(wait = true)
    }
