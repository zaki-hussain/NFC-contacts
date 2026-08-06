package com.nfccard.app

import org.json.JSONArray
import org.json.JSONObject

enum class ProfileKind { LINKEDIN, WHATSAPP, CUSTOM }

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
                Profile(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    url = o.optString("url"),
                    kind = runCatching { ProfileKind.valueOf(o.optString("kind")) }
                        .getOrDefault(ProfileKind.CUSTOM)
                )
            }.filter { it.id.isNotEmpty() && it.url.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
