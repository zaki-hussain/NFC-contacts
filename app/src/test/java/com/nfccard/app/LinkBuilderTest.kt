package com.nfccard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkBuilderTest {

    @Test
    fun `linkedin from username`() =
        assertEquals("https://www.linkedin.com/in/zaki", LinkBuilder.linkedIn("zaki"))

    @Test
    fun `linkedin strips at-sign`() =
        assertEquals("https://www.linkedin.com/in/zaki", LinkBuilder.linkedIn("@zaki"))

    @Test
    fun `linkedin full url passes through`() =
        assertEquals(
            "https://www.linkedin.com/in/zaki",
            LinkBuilder.linkedIn("https://www.linkedin.com/in/zaki")
        )

    @Test
    fun `linkedin url without scheme gets https`() =
        assertEquals(
            "https://linkedin.com/in/zaki",
            LinkBuilder.linkedIn("linkedin.com/in/zaki")
        )

    @Test
    fun `linkedin rejects unrelated url`() =
        assertNull(LinkBuilder.linkedIn("https://example.com/zaki"))

    @Test
    fun `linkedin rejects lookalike hosts and query mentions`() {
        assertNull(LinkBuilder.linkedIn("https://example.com/?ref=linkedin.com"))
        assertNull(LinkBuilder.linkedIn("notlinkedin.com.evil.io/x"))
    }

    @Test
    fun `linkedin salvages url from pasted text`() =
        assertEquals(
            "https://www.linkedin.com/in/jane",
            LinkBuilder.linkedIn("Check out my profile: https://www.linkedin.com/in/jane")
        )

    @Test
    fun `linkedin rejects empty`() = assertNull(LinkBuilder.linkedIn("  "))

    @Test
    fun `whatsapp formats international number`() =
        assertEquals("https://wa.me/447700900123", LinkBuilder.whatsApp("+44 7700 900123"))

    @Test
    fun `whatsapp accepts existing wa me link`() =
        assertEquals("https://wa.me/447700900123", LinkBuilder.whatsApp("https://wa.me/447700900123"))

    @Test
    fun `whatsapp rejects too short`() = assertNull(LinkBuilder.whatsApp("123"))

    @Test
    fun `whatsapp rejects letters mixed in`() = assertNull(LinkBuilder.whatsApp("abc123456"))

    @Test
    fun `whatsapp rejects national format with leading zero`() =
        assertNull(LinkBuilder.whatsApp("07700 900123"))

    @Test
    fun `whatsapp normalizes eastern arabic digits to ascii`() =
        assertEquals("https://wa.me/201234567890", LinkBuilder.whatsApp("+٢٠١٢٣٤٥٦٧٨٩٠"))

    @Test
    fun `custom adds scheme`() =
        assertEquals("https://example.com", LinkBuilder.custom("example.com"))

    @Test
    fun `custom keeps explicit scheme`() =
        assertEquals("http://example.com", LinkBuilder.custom("http://example.com"))

    @Test
    fun `custom keeps mailto and tel schemes`() {
        assertEquals("mailto:me@example.com", LinkBuilder.custom("mailto:me@example.com"))
        assertEquals("tel:+447700900123", LinkBuilder.custom("tel:+447700900123"))
    }

    @Test
    fun `custom rejects interior whitespace`() =
        assertNull(LinkBuilder.custom("my portfolio.example.com"))

    @Test
    fun `custom rejects blank`() = assertNull(LinkBuilder.custom("   "))
}
