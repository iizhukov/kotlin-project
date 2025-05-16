package features.stats

import kotlinx.serialization.Serializable

@Serializable
data class StatsResponse(
    val endpoints: Map<String, EndpointStats>,
    val totalRequests: Int,
    val totalSuccessful: Int,
    val totalFailed: Int,
    val totalCached: Int,
    val timestamp: Long,
    val sinceStartup: Long,
)

@Serializable
data class EndpointStats(
    val requests: Int,
    val successful: Int,
    val failed: Int,
    val cached: Int,
    val lastUpdated: Long,
)

@Serializable
data class HistoryStatsResponse(
    val records: List<StatsModel>,
    val totalCount: Long,
    val currentPage: Int,
    val totalPages: Long,
)

@Serializable
data class StatsModel(
    val endpoint: String,
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val cachedRequests: Int,
    val availableEndpoints: Int,
    val timestamp: Long,
)
