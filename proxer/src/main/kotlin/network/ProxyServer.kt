package com.iizhukov.network

import com.iizhukov.config.Config
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Future

class ProxyServer(private val config: Config) {
    suspend fun run() = coroutineScope {
        val serverChannel = AsynchronousServerSocketChannel.open()
        serverChannel.bind(InetSocketAddress(config.port))

        while (true) {
            val client = serverChannel.accept()

            launch {
                handleRequest(client.get())
            }
        }
    }

    suspend fun handleRequest(clientChannel: AsynchronousSocketChannel) {
        val buffer = ByteBuffer.allocate(8192)
        val bytesRead = clientChannel.read(buffer)

        TODO()

    }
}