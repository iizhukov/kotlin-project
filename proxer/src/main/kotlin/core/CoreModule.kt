@file:Suppress("ktlint:standard:no-wildcard-imports")

package core

import config.Config
import features.proxy.LoadBalancerFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreModule(config: Config): Module =
    module {
        single { config }
        single { HttpClient(CIO) }
        single { LoadBalancerFactory() }
    }
