package com.nfccard.app

/** Turns user input into shareable URLs. Returns null for input that can't work. */
object LinkBuilder {

    private val SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val WHITESPACE = Regex("\\s+")

    /** Accepts a full LinkedIn URL (even inside pasted text) or a bare username. */
    fun linkedIn(input: String): String? {
        val t = input.trim()
        if (t.isEmpty()) return null
        // Salvage the URL token from pasted text ("Check out my profile: https://…")
        t.split(WHITESPACE).firstOrNull { isLinkedInUrl(it) }?.let { return ensureScheme(it) }
        // Otherwise treat as a bare username
        return when {
            t.contains("://") || t.contains('/') || t.any { it.isWhitespace() } -> null
            else -> "https://www.linkedin.com/in/${t.trimStart('@')}"
        }
    }

    /** Accepts a phone number in any common format, or an existing wa.me link. */
    fun whatsApp(input: String): String? {
        val t = input.trim()
        val waMe = t.lowercase().indexOf("wa.me/")
        val candidate = if (waMe >= 0) {
            t.substring(waMe + "wa.me/".length).substringBefore('?').substringBefore('/')
        } else {
            // Must look like a phone number: digits plus common formatting only
            if (t.any { !it.isDigit() && it !in "+-(). " }) return null
            t
        }
        // Normalize any Unicode digit (e.g. Eastern Arabic) to ASCII, drop formatting
        val digits = candidate
            .mapNotNull { c -> c.digitToIntOrNull()?.digitToChar() }
            .joinToString("")
        // wa.me needs international format: at least 6 digits, no leading zero
        return if (digits.length >= 6 && !digits.startsWith("0")) "https://wa.me/$digits" else null
    }

    /** Any link; adds https:// when no scheme is given. */
    fun custom(input: String): String? {
        val t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() || it.isISOControl() }) return null
        return ensureScheme(t)
    }

    private fun isLinkedInUrl(s: String): Boolean {
        val lower = s.lowercase()
        val host = lower.substringAfter("://", lower)
            .substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        return host == "linkedin.com" || host.endsWith(".linkedin.com")
    }

    private fun ensureScheme(s: String) = if (SCHEME.containsMatchIn(s)) s else "https://$s"
}
