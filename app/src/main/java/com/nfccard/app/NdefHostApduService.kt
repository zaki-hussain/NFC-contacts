package com.nfccard.app

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Emulates an NFC Forum Type 4 Tag holding a single NDEF URI record.
 * A reader phone tapping this device selects the NDEF application
 * (AID D2760000850101), reads the capability container, then reads the
 * NDEF file — same flow as reading a physical NFC tag.
 */
class NdefHostApduService : HostApduService() {

    private var tag: Type4Tag? = null

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        val tag = this.tag ?: run {
            val url = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                .getString(Prefs.KEY_URL, null) ?: ""
            Type4Tag(url).also { this.tag = it }
        }
        return tag.process(apdu)
    }

    override fun onDeactivated(reason: Int) {
        tag = null
    }
}
