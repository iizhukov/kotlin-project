package core

import config.CacheConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import org.koin.dsl.module

@OptIn(ExperimentalLettuceCoroutinesApi::class)
val cacheModule =
    module {
        single {
            val cacheConfig = get<CacheConfig>().redis
            RedisClient
                .create("redis://${cacheConfig.password}@${cacheConfig.host}:${cacheConfig.port}/${cacheConfig.index}")
                .connect()
                .coroutines()
        }
    }
