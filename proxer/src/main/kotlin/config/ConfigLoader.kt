package com.iizhukov.config

import kotlinx.serialization.json.Json
import java.io.File

class ConfigLoader {
    companion object {
        @Volatile
        private var instance: ConfigLoader? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ConfigLoader().also { instance = it }
            }
    }

    fun loadConfig(path: String): Config {
        val configText = File(path).readText()
        return Json.decodeFromString(configText)
    }
}