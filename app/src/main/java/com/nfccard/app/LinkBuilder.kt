package com.nfccard.app

/** Turns user input into shareable URLs. Returns null for input that can't work. */
object LinkBuilder {

    /** Accepts a full LinkedIn URL or a bare username. */
    fun linkedIn(input: String): String? {
        val t = input.trim()
        if (t.isEmpty()) return null
        val lower = t.lowercase()
        return when {
            lower.contains("linkedin.com") -> ensureScheme(t)
            // Not a linkedin.com URL and not a plausible bare username
            lower.contains("://") || lower.contains("/") || lower.contains(" ") -> null
            else -> "https://www.linkedin.com/in/${t.trimStart('@')}"
        }
    }

    /** Accepts a phone number in any format (or an existing wa.me link). */
    fun whatsApp(input: String): String? {
        val digits = input.filter { it.isDigit() }
        return if (digits.length < 6) null else "https://wa.me/$digits"
    }

    /** Any link; adds https:// when no scheme is given. */
    fun custom(input: String): String? {
        val t = input.trim()
        if (t.isEmpty()) return null
        return ensureScheme(t)
    }

    private fun ensureScheme(s: String) = if (s.contains("://")) s else "https://$s"
}
