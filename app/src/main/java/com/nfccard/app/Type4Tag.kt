package com.nfccard.app

/**
 * NFC Forum Type 4 Tag protocol state machine, independent of Android
 * classes so it can be unit tested. Serves a capability container file
 * and a read-only NDEF file containing the given NDEF message.
 */
class Type4Tag(ndefMessage: ByteArray) {

    companion object {
        val OK = byteArrayOf(0x90.toByte(), 0x00)
        val ERROR = byteArrayOf(0x6A.toByte(), 0x82.toByte()) // file not found

        val NDEF_APP_AID = hex("D2760000850101")
        const val CC_FILE_ID = 0xE103
        const val NDEF_FILE_ID = 0xE104

        // Capability container: v2.0, MLe/MLc 255, NDEF file E104,
        // max size 0x0FFF, read allowed, write forbidden
        val CC_FILE = hex("000F2000FF00FF0406E1040FFF00FF")

        fun hex(s: String): ByteArray =
            s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private enum class SelectedFile { NONE, CC, NDEF }

    private var selectedFile = SelectedFile.NONE

    /** NDEF file: 2-byte length prefix followed by the NDEF message.
     *  An empty message yields NLEN=0 — a valid empty tag readers cleanly ignore. */
    private val ndefFile: ByteArray = byteArrayOf(
        (ndefMessage.size shr 8).toByte(),
        (ndefMessage.size and 0xFF).toByte()
    ) + ndefMessage

    fun process(apdu: ByteArray): ByteArray {
        // SELECT: 00 A4 <p1> <p2> <lc> <data...> [le]
        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()) {
            val lc = apdu[4].toInt() and 0xFF
            if (apdu.size < 5 + lc) return ERROR
            val data = apdu.copyOfRange(5, 5 + lc)
            // SELECT by AID (P1=04)
            if (apdu[2] == 0x04.toByte() && data.contentEquals(NDEF_APP_AID)) {
                selectedFile = SelectedFile.NONE
                return OK
            }
            // SELECT file by ID (P1=00)
            if (apdu[2] == 0x00.toByte() && lc == 2) {
                val fileId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                when (fileId) {
                    CC_FILE_ID -> { selectedFile = SelectedFile.CC; return OK }
                    NDEF_FILE_ID -> { selectedFile = SelectedFile.NDEF; return OK }
                }
            }
            return ERROR
        }
        // READ BINARY: 00 B0 <offset:2> <le:1>
        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xB0.toByte()) {
            val file = when (selectedFile) {
                SelectedFile.CC -> CC_FILE
                SelectedFile.NDEF -> ndefFile
                SelectedFile.NONE -> return ERROR
            }
            val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
            var le = apdu[4].toInt() and 0xFF
            if (le == 0) le = 256
            if (offset > file.size) return ERROR
            val end = minOf(offset + le, file.size)
            return file.copyOfRange(offset, end) + OK
        }
        return ERROR
    }

    fun reset() {
        selectedFile = SelectedFile.NONE
    }
}
