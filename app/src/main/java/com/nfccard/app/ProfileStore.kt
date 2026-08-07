package com.nfccard.app

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/** What is currently being shared: a URL, or vCard text for contact cards. */
data class Share(val payload: String, val isVcard: Boolean)

/**
 * Persistence for profiles and the currently shared link.
 * The shared link is the one-off URL if set, otherwise the active profile's URL.
 */
object ProfileStore {
    private const val PREFS = "nfc_card"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_ACTIVE = "active_id"
    private const val KEY_ONE_OFF = "one_off_url"
    private const val LEGACY_KEY_URL = "url"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun profiles(context: Context): List<Profile> {
        migrateLegacyUrl(context)
        return Profile.listFromJson(prefs(context).getString(KEY_PROFILES, "") ?: "")
    }

    fun saveProfiles(context: Context, profiles: List<Profile>) {
        prefs(context).edit().putString(KEY_PROFILES, Profile.listToJson(profiles)).apply()
    }

    fun activeId(context: Context): String? = prefs(context).getString(KEY_ACTIVE, null)

    fun setActive(context: Context, id: String?) {
        prefs(context).edit().putString(KEY_ACTIVE, id).apply()
    }

    /** User picked a profile: it becomes the shared link, replacing any one-off. */
    fun selectProfile(context: Context, id: String) {
        prefs(context).edit().putString(KEY_ACTIVE, id).remove(KEY_ONE_OFF).apply()
    }

    fun oneOffUrl(context: Context): String? =
        prefs(context).getString(KEY_ONE_OFF, null)?.takeIf { it.isNotBlank() }

    fun setOneOff(context: Context, url: String) {
        prefs(context).edit().putString(KEY_ONE_OFF, url).apply()
    }

    fun clearOneOff(context: Context) {
        prefs(context).edit().remove(KEY_ONE_OFF).apply()
    }

    /** What's currently served over NFC and shown as QR, or null if nothing. */
    fun currentShare(context: Context): Share? {
        oneOffUrl(context)?.let { return Share(it, isVcard = false) }
        val active = profiles(context).find { it.id == activeId(context) } ?: return null
        if (active.url.isBlank()) return null
        return Share(active.url, isVcard = active.kind == ProfileKind.VCARD)
    }

    /** v0.1 stored a single URL under "url"; turn it into a custom profile once. */
    private fun migrateLegacyUrl(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_PROFILES)) return
        val legacy = p.getString(LEGACY_KEY_URL, null)?.trim().orEmpty()
        if (legacy.isEmpty()) return
        val profile = Profile(UUID.randomUUID().toString(), "My link", legacy, ProfileKind.CUSTOM)
        p.edit()
            .putString(KEY_PROFILES, Profile.listToJson(listOf(profile)))
            .putString(KEY_ACTIVE, profile.id)
            .remove(LEGACY_KEY_URL)
            .apply()
    }
}
