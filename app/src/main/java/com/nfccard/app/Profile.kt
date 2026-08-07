package com.nfccard.app

import org.json.JSONArray
import org.json.JSONObject

enum class ProfileKind { LINKEDIN, WHATSAPP, CUSTOM, VCARD }

data class Profile(
    val id: String,
    val name: String,
    val url: String,
    val kind: ProfileKind
) {
    companion object {
        fun listToJson(profiles: List<Profile>): String {
            val arr = JSONArray()
            for (p in profiles) {
                arr.put(
                    JSONObject()
                        .put("id", p.id)
                        .put("name", p.name)
                        .put("url", p.url)
                        .put("kind", p.kind.name)
                )
            }
            return arr.toString()
        }

        fun listFromJson(json: String): List<Profile> = try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                // isNull-guarded: Android's optString returns "null" for JSON nulls
                fun str(key: String) = if (o.isNull(key)) "" else o.optString(key)
                Profile(
                    id = str("id"),
                    name = str("name"),
                    url = str("url"),
                    kind = runCatching { ProfileKind.valueOf(str("kind")) }
                        .getOrDefault(ProfileKind.CUSTOM)
                )
            }.filter { it.id.isNotEmpty() && it.url.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
