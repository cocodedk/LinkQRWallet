package com.cocode.linkqrwallet

import java.io.FilterInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import javax.net.SocketFactory

/** Counts the bytes the client reads from its sockets. */
class CountingSocketFactory : SocketFactory() {
    val bytesRead = AtomicLong()

    private inner class CountingSocket : Socket() {
        override fun getInputStream(): InputStream = object : FilterInputStream(super.getInputStream()) {
            override fun read(): Int = super.read().also { if (it >= 0) bytesRead.incrementAndGet() }

            override fun read(b: ByteArray, off: Int, len: Int): Int =
                super.read(b, off, len).also { if (it > 0) bytesRead.addAndGet(it.toLong()) }
        }
    }

    override fun createSocket(): Socket = CountingSocket()

    override fun createSocket(host: String, port: Int): Socket =
        CountingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: String, port: Int, local: InetAddress, localPort: Int): Socket =
        CountingSocket().apply { bind(InetSocketAddress(local, localPort)); connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: InetAddress, port: Int): Socket =
        CountingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(address: InetAddress, port: Int, local: InetAddress, localPort: Int): Socket =
        CountingSocket().apply { bind(InetSocketAddress(local, localPort)); connect(InetSocketAddress(address, port)) }
}
