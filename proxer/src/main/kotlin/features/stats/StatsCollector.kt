@file:Suppress("ktlint:standard:no-wildcard-imports")

package features.stats

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoCollection
import config.StatsConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class StatsCollector(
    private val config: StatsConfig,
    private val collection: MongoCollection<StatsModel>,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val statsChannel = Channel<StatsEvent>(Channel.UNLIMITED)
    private val currentStats = ConcurrentHashMap<String, EndpointStatsData>()
    private val mutex = Mutex()
    private val startupTime = System.currentTimeMillis()

    private data class EndpointStatsData(
        var requests: Int = 0,
        var successful: Int = 0,
        var failed: Int = 0,
        var cached: Int = 0,
    )

    sealed class StatsEvent {
        abstract val endpoint: String

        data class Request(
            override val endpoint: String,
        ) : StatsEvent()

        data class Success(
            override val endpoint: String,
        ) : StatsEvent()

        data class Failure(
            override val endpoint: String,
        ) : StatsEvent()

        data class Cached(
            override val endpoint: String,
        ) : StatsEvent()
    }

    init {
        startCollector()
        startPersister()
    }

    fun trackRequest(endpoint: String) {
        scope.launch {
            statsChannel.send(StatsEvent.Request(endpoint))
        }
    }

    fun trackSuccess(endpoint: String) {
        scope.launch {
            statsChannel.send(StatsEvent.Success(endpoint))
        }
    }

    fun trackFailure(endpoint: String) {
        scope.launch {
            statsChannel.send(StatsEvent.Failure(endpoint))
        }
    }

    fun trackCached(endpoint: String) {
        scope.launch {
            statsChannel.send(StatsEvent.Cached(endpoint))
        }
    }

    suspend fun getStats(): StatsResponse =
        mutex.withLock {
            val endpoints =
                currentStats.mapValues { (_, stats) ->
                    EndpointStats(
                        requests = stats.requests,
                        successful = stats.successful,
                        failed = stats.failed,
                        cached = stats.cached,
                        lastUpdated = System.currentTimeMillis(),
                    )
                }

            StatsResponse(
                endpoints = endpoints,
                totalRequests = endpoints.values.sumOf { it.requests },
                totalSuccessful = endpoints.values.sumOf { it.successful },
                totalFailed = endpoints.values.sumOf { it.failed },
                totalCached = endpoints.values.sumOf { it.cached },
                timestamp = System.currentTimeMillis(),
                sinceStartup = System.currentTimeMillis() - startupTime,
            )
        }

    suspend fun getHistory(
        page: Int = 1,
        pageSize: Int = 10,
        endpointFilter: String? = null,
    ): HistoryStatsResponse {
        val skip = (page - 1) * pageSize
        val filter = endpointFilter?.let { Filters.eq("endpoint", it) } ?: Filters.empty()

        val records =
            collection
                .find(filter)
                .sort(Sorts.descending("timestamp"))
                .skip(skip)
                .limit(pageSize)
                .toList()

        val totalCount = collection.countDocuments(filter)

        return HistoryStatsResponse(
            records = records,
            totalCount = totalCount,
            currentPage = page,
            totalPages = (totalCount + pageSize - 1) / pageSize,
        )
    }

    private fun startCollector() {
        scope.launch {
            for (event in statsChannel) {
                mutex.withLock {
                    val stats = currentStats.getOrPut(event.endpoint) { EndpointStatsData() }
                    when (event) {
                        is StatsEvent.Request -> stats.requests++
                        is StatsEvent.Success -> stats.successful++
                        is StatsEvent.Failure -> stats.failed++
                        is StatsEvent.Cached -> stats.cached++
                    }
                }
            }
        }
    }

    private fun startPersister() {
        scope.launch {
            while (isActive) {
                delay(config.frequency.toLong() * 1000)
                persistStats()
            }
        }
    }

    private suspend fun persistStats() {
        val statsToPersist =
            mutex.withLock {
                currentStats
                    .map { (endpoint, stats) ->
                        StatsModel(
                            endpoint = endpoint,
                            totalRequests = stats.requests,
                            successfulRequests = stats.successful,
                            failedRequests = stats.failed,
                            cachedRequests = stats.cached,
                            availableEndpoints = currentStats.size,
                            timestamp = System.currentTimeMillis(),
                        )
                    }.also { currentStats.clear() }
            }

        if (statsToPersist.isNotEmpty()) {
            println("added")
            collection.insertMany(statsToPersist)
        } else {
            println("not added")
        }
    }
}
