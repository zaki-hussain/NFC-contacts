package com.nfccard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardTest {

    private val sample =
        "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Zaki Hussain\r\nTEL:+447700900123\r\nEND:VCARD"

    @Test
    fun `sanitize accepts a valid vcard`() = assertEquals(sample, VCard.sanitize(sample))

    @Test
    fun `sanitize normalizes bare newlines`() =
        assertEquals(sample, VCard.sanitize(sample.replace("\r\n", "\n")))

    @Test
    fun `sanitize rejects non vcard text`() {
        assertNull(VCard.sanitize("hello world"))
        assertNull(VCard.sanitize(""))
    }

    @Test
    fun `sanitize strips photo including folded lines`() {
        val withPhoto = "BEGIN:VCARD\nVERSION:3.0\nFN:Z\n" +
            "PHOTO;ENCODING=b;TYPE=JPEG:AAAABBBB\n CCCCDDDD\n EEEEFFFF\n" +
            "TEL:+123456\nEND:VCARD"
        val clean = VCard.sanitize(withPhoto)!!
        assertFalse(clean.contains("PHOTO"))
        assertFalse(clean.contains("CCCC"))
        assertTrue(clean.contains("FN:Z"))
        assertTrue(clean.contains("TEL:+123456"))
    }

    @Test
    fun `display name from fn`() = assertEquals("Zaki Hussain", VCard.displayName(sample))

    @Test
    fun `display name from fn with parameters`() =
        assertEquals(
            "Zaki",
            VCard.displayName("BEGIN:VCARD\r\nFN;CHARSET=UTF-8:Zaki\r\nEND:VCARD")
        )

    @Test
    fun `display name absent gives null`() =
        assertNull(VCard.displayName("BEGIN:VCARD\r\nTEL:+123\r\nEND:VCARD"))
}
