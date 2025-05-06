package config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object ConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadConfig(path: String): Config =
        withContext(Dispatchers.IO) {
            json.decodeFromString<Config>(File(path).readText())
        }
}
