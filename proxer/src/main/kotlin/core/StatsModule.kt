package core

import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import config.Config
import config.StatsConfig
import features.stats.StatsCollector
import features.stats.StatsModel
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module

val statsModule =
    module {
        single { get<Config>().stats }
        single {
            val mongo = get<StatsConfig>().mongodb
            MongoClient.create(
                "mongodb://${mongo.username}:${mongo.password}@${mongo.host}:${mongo.port}/?authSource=admin",
            )
        }
        single {
            val config = get<StatsConfig>().mongodb
            get<MongoClient>().getDatabase(config.dbname)
        }

        single {
            val db = get<MongoDatabase>()
            val collection = db.getCollection<StatsModel>("stats")

            runBlocking {
                collection.createIndex(Indexes.descending("timestamp"))
                collection.createIndex(Indexes.descending("endpoint"))
            }

            collection
        }

        single {
            val config = get<StatsConfig>()
            val collection = get<MongoCollection<StatsModel>>()

            StatsCollector(config, collection)
        }
    }
