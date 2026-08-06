package com.nfccard.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Simulates the APDU sequence a reader phone performs during a tap. */
class Type4TagTest {

    private fun hex(s: String) = Type4Tag.hex(s.replace(" ", ""))

    private fun status(response: ByteArray) =
        response.copyOfRange(response.size - 2, response.size)

    private fun data(response: ByteArray) =
        response.copyOfRange(0, response.size - 2)

    /** Full read flow: select app, read CC, select + read NDEF file. */
    private fun readTag(tag: Type4Tag): ByteArray {
        // SELECT NDEF app (with trailing Le byte, as Android readers send it)
        assertArrayEquals(Type4Tag.OK, tag.process(hex("00 A4 0400 07 D2760000850101 00")))

        // SELECT + read capability container
        assertArrayEquals(Type4Tag.OK, tag.process(hex("00 A4 000C 02 E103")))
        val cc = tag.process(hex("00 B0 0000 0F"))
        assertArrayEquals(Type4Tag.OK, status(cc))
        assertArrayEquals(Type4Tag.CC_FILE, data(cc))

        // SELECT NDEF file, read 2-byte length, then read the message in chunks
        assertArrayEquals(Type4Tag.OK, tag.process(hex("00 A4 000C 02 E104")))
        val lenResp = tag.process(hex("00 B0 0000 02"))
        assertArrayEquals(Type4Tag.OK, status(lenResp))
        val nlen = ((data(lenResp)[0].toInt() and 0xFF) shl 8) or
            (data(lenResp)[1].toInt() and 0xFF)

        val message = ByteArray(nlen)
        var read = 0
        while (read < nlen) {
            val chunk = minOf(0xFF, nlen - read)
            val resp = tag.process(
                hex("00 B0") + byteArrayOf(
                    ((read + 2) shr 8).toByte(), ((read + 2) and 0xFF).toByte(),
                    chunk.toByte()
                )
            )
            assertArrayEquals(Type4Tag.OK, status(resp))
            data(resp).copyInto(message, read)
            read += data(resp).size
        }
        return message
    }

    @Test
    fun `reader flow yields URI record for short url`() {
        val message = readTag(Type4Tag("https://example.com"))
        // D1 01 <len> 55 00 <url>
        assertEquals(0xD1.toByte(), message[0])
        assertEquals(0x55.toByte(), message[3])
        assertEquals(0x00.toByte(), message[4]) // no URI abbreviation
        assertEquals("https://example.com", String(message.copyOfRange(5, message.size)))
    }

    @Test
    fun `reader flow yields URI record for long url`() {
        val url = "https://example.com/" + "x".repeat(400)
        val message = readTag(Type4Tag(url))
        // C1 01 <len:4> 55 00 <url>
        assertEquals(0xC1.toByte(), message[0])
        assertEquals(0x55.toByte(), message[6])
        assertEquals(url, String(message.copyOfRange(8, message.size)))
    }

    @Test
    fun `select by aid also accepted without trailing le byte`() {
        val tag = Type4Tag("https://a.b")
        assertArrayEquals(Type4Tag.OK, tag.process(hex("00 A4 0400 07 D2760000850101")))
    }

    @Test
    fun `read before select fails`() {
        val tag = Type4Tag("https://a.b")
        assertArrayEquals(Type4Tag.ERROR, tag.process(hex("00 B0 0000 0F")))
    }

    @Test
    fun `unknown aid rejected`() {
        val tag = Type4Tag("https://a.b")
        assertArrayEquals(Type4Tag.ERROR, tag.process(hex("00 A4 0400 07 A0000000000000")))
    }

    @Test
    fun `cc file declares readable non-writable ndef file`() {
        val cc = Type4Tag.CC_FILE
        assertEquals(15, cc.size)
        assertTrue(cc[13] == 0x00.toByte()) // read allowed
        assertTrue(cc[14] == 0xFF.toByte()) // write forbidden
    }
}
