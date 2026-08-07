package com.nfccard.app

/** Builders for single-record NDEF messages. */
object Ndef {

    /** Well-known URI record (type "U", identifier code 0x00 = full URI). */
    fun uriMessage(url: String): ByteArray =
        record(tnf = 0x01, type = byteArrayOf(0x55), payload = byteArrayOf(0x00) + url.toByteArray(Charsets.UTF_8))

    /** MIME media record, e.g. text/vcard. */
    fun mimeMessage(mimeType: String, payload: ByteArray): ByteArray =
        record(tnf = 0x02, type = mimeType.toByteArray(Charsets.US_ASCII), payload = payload)

    private fun record(tnf: Int, type: ByteArray, payload: ByteArray): ByteArray {
        val short = payload.size < 256
        // MB | ME | SR? | TNF
        val flags = (0x80 or 0x40 or (if (short) 0x10 else 0) or tnf).toByte()
        val payloadLen = if (short) {
            byteArrayOf(payload.size.toByte())
        } else {
            byteArrayOf(
                (payload.size ushr 24).toByte(),
                (payload.size ushr 16).toByte(),
                (payload.size ushr 8).toByte(),
                payload.size.toByte()
            )
        }
        return byteArrayOf(flags, type.size.toByte()) + payloadLen + type + payload
    }
}
