package com.iizhukov.config

import kotlinx.serialization.*


@Serializable
data class RouteConfig(val prefix: String, val target: String)

@Serializable
data class Config(val port: Int, val routes: List<RouteConfig>)
