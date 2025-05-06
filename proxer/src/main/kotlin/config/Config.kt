package config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val host: String,
    val port: Int,
)

@Serializable
data class ProxyConfig(
    val prefix: String,
    val target: String,
    val protocol: String,
    val ips: List<String>,
    val cache: Boolean,
)

@Serializable
data class QueueConfig(
    val prefix: String,
    val target: String,
    val ip: String,
    val protocol: String,
)

@Serializable
data class MongoConfig(
    val username: String,
    val password: String,
    val host: String,
    val port: Int,
    val dbname: String,
)

@Serializable
data class StatsConfig(
    val enabled: Boolean,
    val port: Int,
    val frequency: Int,
    val mongodb: MongoConfig,
)

@Serializable
data class RedisConfig(
    val password: String,
    val host: String,
    val port: Int,
    val index: Int,
)

@Serializable
data class CacheConfig(
    val enabled: Boolean,
    val duration: Int,
    val redis: RedisConfig,
)

@Serializable
data class Config(
    val server: ServerConfig,
    val proxy: List<ProxyConfig>,
    val queues: List<QueueConfig>,
    val stats: StatsConfig,
    val cache: CacheConfig,
)
