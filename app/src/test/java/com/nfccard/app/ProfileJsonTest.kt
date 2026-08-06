package com.nfccard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileJsonTest {

    @Test
    fun `round trip preserves profiles`() {
        val profiles = listOf(
            Profile("1", "LinkedIn", "https://www.linkedin.com/in/zaki", ProfileKind.LINKEDIN),
            Profile("2", "WhatsApp", "https://wa.me/447700900123", ProfileKind.WHATSAPP),
            Profile("3", "Portfolio — \"dev\"", "https://example.com?a=1&b=2", ProfileKind.CUSTOM)
        )
        assertEquals(profiles, Profile.listFromJson(Profile.listToJson(profiles)))
    }

    @Test
    fun `unknown kind becomes custom`() {
        val json = """[{"id":"1","name":"X","url":"https://x.com","kind":"TIKTOK"}]"""
        assertEquals(ProfileKind.CUSTOM, Profile.listFromJson(json).single().kind)
    }

    @Test
    fun `corrupt json gives empty list`() {
        assertTrue(Profile.listFromJson("not json").isEmpty())
        assertTrue(Profile.listFromJson("").isEmpty())
    }

    @Test
    fun `explicit json nulls are dropped`() {
        val json = """[{"id":"1","name":null,"url":null,"kind":null}]"""
        assertTrue(Profile.listFromJson(json).isEmpty())
    }

    @Test
    fun `entries missing id or url are dropped`() {
        val json = """[{"id":"","name":"a","url":"https://x.com","kind":"CUSTOM"},
                       {"id":"2","name":"b","url":"","kind":"CUSTOM"},
                       {"id":"3","name":"c","url":"https://ok.com","kind":"CUSTOM"}]"""
        assertEquals(listOf("3"), Profile.listFromJson(json).map { it.id })
    }
}
