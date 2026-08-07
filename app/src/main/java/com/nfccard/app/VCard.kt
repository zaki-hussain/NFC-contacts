package com.nfccard.app

/** Minimal vCard handling: validation, photo stripping, display-name extraction. */
object VCard {

    /**
     * Validates and cleans raw vCard text: normalizes line endings and drops
     * PHOTO properties (their size would break NFC's 4KB file and QR capacity).
     * Returns null if the text isn't a vCard.
     */
    fun sanitize(raw: String): String? {
        val text = raw.trim().replace("\r\n", "\n").replace('\r', '\n')
        if (!text.startsWith("BEGIN:VCARD", ignoreCase = true) ||
            !text.uppercase().contains("END:VCARD")
        ) return null
        val out = mutableListOf<String>()
        var skipping = false
        for (line in text.split('\n')) {
            val continuation = line.startsWith(" ") || line.startsWith("\t")
            if (continuation) {
                if (skipping) continue
            } else {
                skipping = line.startsWith("PHOTO", ignoreCase = true)
                if (skipping) continue
            }
            out.add(line)
        }
        return out.joinToString("\r\n")
    }

    /** The FN (formatted name) property value, if present. */
    fun displayName(vcard: String): String? =
        vcard.replace("\r\n", "\n").replace('\r', '\n')
            .replace(Regex("\n[ \t]"), "") // unfold continuation lines
            .lineSequence()
            .firstOrNull {
                it.startsWith("FN", ignoreCase = true) &&
                    (it.getOrNull(2) == ':' || it.getOrNull(2) == ';')
            }
            ?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }
}
