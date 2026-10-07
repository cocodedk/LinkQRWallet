package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.CheckedDns
import java.net.InetAddress
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** The addresses that pass the check are the ones OkHttp connects to, and one bad answer refuses the name. */
class CheckedDnsTest {
    private fun dns(vararg answers: String) = CheckedDns { answers.map { InetAddress.getByName(it) } }

    @Test
    fun publicAnswersAreHandedOnUnchanged() {
        val answers = arrayOf("93.184.216.34", "2606:4700:4700::1111")
        assertEquals(answers.map { InetAddress.getByName(it) }, dns(*answers).lookup("example.com"))
    }

    @Test
    fun aMixedPublicAndPrivateAnswerRefusesTheWholeName() {
        for (local in listOf("127.0.0.1", "10.1.2.3", "192.168.1.1", "100.64.0.1", "::1", "fd00::1", "fe80::1")) {
            assertThrows(local, UnknownHostException::class.java) {
                dns("93.184.216.34", local).lookup("sneaky.example.com")
            }
            assertThrows(local, UnknownHostException::class.java) {
                dns(local, "93.184.216.34").lookup("sneaky.example.com")
            }
        }
    }

    @Test
    fun aNameThatLeadsOnlyToAPrivateAddressIsRefused() {
        assertThrows(UnknownHostException::class.java) { dns("172.16.5.5").lookup("intranet.example.net") }
    }

    @Test
    fun aNameWithNoAnswerIsRefused() {
        assertThrows(UnknownHostException::class.java) { dns().lookup("nowhere.example.com") }
    }

    @Test
    fun aResolverFailureIsPassedOn() {
        val failing = CheckedDns { throw UnknownHostException(it) }
        assertThrows(UnknownHostException::class.java) { failing.lookup("nowhere.example.com") }
    }

    @Test
    fun namesThatTheSystemReadsAsLocalNumbersAreRefused() {
        // the system turns each of these spellings into 127.0.0.1 before the check sees it
        val local = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        val dns = CheckedDns { listOf(local) }
        for (name in listOf("2130706433", "0177.0.0.1", "0x7f.0.0.1", "0x7f000001", "127.1")) {
            assertThrows(name, UnknownHostException::class.java) { dns.lookup(name) }
        }
    }
}
