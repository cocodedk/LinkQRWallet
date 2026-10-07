package com.cocode.linkqrwallet.data

import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns

/** Finds which addresses a website name leads to. Blocks, so call it off the main thread. */
fun interface HostResolver {
    fun resolve(host: String): List<InetAddress>
}

/**
 * Resolves a name once and hands OkHttp only the addresses it checked, so the connection goes
 * to exactly the addresses that passed. If any address the name leads to is refused by
 * [AddressRules], the whole name is refused: nothing is dropped quietly.
 */
class CheckedDns(
    private val resolver: HostResolver = HostResolver { InetAddress.getAllByName(it).toList() }
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = resolver.resolve(hostname)
        if (addresses.isEmpty()) throw UnknownHostException(hostname)
        if (addresses.any { AddressRules.isBlocked(it) }) {
            throw UnknownHostException("$hostname leads to an address the app refuses to contact")
        }
        return addresses
    }
}
