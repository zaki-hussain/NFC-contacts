package com.nfccard.app

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlInput = findViewById<EditText>(R.id.url_input)
        val saveButton = findViewById<Button>(R.id.save_button)
        val status = findViewById<TextView>(R.id.status)

        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        urlInput.setText(prefs.getString(Prefs.KEY_URL, ""))

        saveButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            prefs.edit().putString(Prefs.KEY_URL, url).apply()
            status.text = if (url.isEmpty()) {
                getString(R.string.status_empty)
            } else {
                getString(R.string.status_ready, url)
            }
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        }

        val savedUrl = prefs.getString(Prefs.KEY_URL, "").orEmpty()
        status.text = if (savedUrl.isEmpty()) {
            getString(R.string.status_empty)
        } else {
            getString(R.string.status_ready, savedUrl)
        }

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            status.text = getString(R.string.no_nfc)
        } else if (!adapter.isEnabled) {
            status.text = getString(R.string.nfc_off)
        }
    }
}
