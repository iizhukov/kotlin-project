package com.iizhukov

import kotlinx.coroutines.runBlocking
import com.iizhukov.config.*

fun main(): Unit = runBlocking {
    val configLoader = ConfigLoader.getInstance()
    val config = configLoader.loadConfig("config.json")

    println(config.port)
}