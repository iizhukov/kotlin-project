package features.proxy

import java.util.concurrent.atomic.AtomicInteger

class RoundRobinLoadBalancer(
    private val servers: List<String>,
) {
    private val counter = AtomicInteger(0)

    fun nextServer(): String = servers[counter.getAndIncrement() % servers.size]
}

class LoadBalancerFactory {
    fun create(servers: List<String>): RoundRobinLoadBalancer = RoundRobinLoadBalancer(servers)
}
