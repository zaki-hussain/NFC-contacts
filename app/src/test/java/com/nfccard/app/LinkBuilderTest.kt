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
    fun `custom adds scheme`() =
        assertEquals("https://example.com", LinkBuilder.custom("example.com"))

    @Test
    fun `custom keeps explicit scheme`() =
        assertEquals("http://example.com", LinkBuilder.custom("http://example.com"))

    @Test
    fun `custom rejects blank`() = assertNull(LinkBuilder.custom("   "))
}
