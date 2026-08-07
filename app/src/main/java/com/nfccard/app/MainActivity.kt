package com.nfccard.app

import android.graphics.drawable.BitmapDrawable
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ProfileAdapter
    private lateinit var qrImage: ImageView
    private lateinit var activeLabel: TextView
    private lateinit var clearOneOff: ImageButton
    private lateinit var currentUrl: TextView
    private lateinit var tapHint: TextView
    private lateinit var nfcWarning: TextView
    private lateinit var emptyView: TextView
    private lateinit var oneOffInput: TextInputEditText
    private lateinit var oneOffLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // NFC card emulation needs the screen on; keep it on while presenting
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        qrImage = findViewById(R.id.qr_image)
        activeLabel = findViewById(R.id.active_label)
        clearOneOff = findViewById(R.id.clear_one_off)
        currentUrl = findViewById(R.id.current_url)
        tapHint = findViewById(R.id.tap_hint)
        nfcWarning = findViewById(R.id.nfc_warning)
        emptyView = findViewById(R.id.empty_view)
        oneOffInput = findViewById(R.id.one_off_input)
        oneOffLayout = findViewById(R.id.one_off_layout)

        adapter = ProfileAdapter(
            onSelect = { profile ->
                ProfileStore.selectProfile(this, profile.id)
                refresh()
            },
            onMenu = { anchor, profile -> showProfileMenu(anchor, profile) }
        )
        findViewById<RecyclerView>(R.id.profile_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<MaterialButton>(R.id.one_off_share).setOnClickListener { shareOneOff() }
        oneOffInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (actionId == EditorInfo.IME_NULL && event?.action == KeyEvent.ACTION_DOWN)
            ) {
                shareOneOff()
                true
            } else {
                // Consume the hardware-Enter key-UP without re-sharing
                actionId == EditorInfo.IME_NULL
            }
        }
        clearOneOff.setOnClickListener {
            ProfileStore.clearOneOff(this)
            refresh()
        }
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            ProfileDialogFragment.show(supportFragmentManager, null)
        }
        qrImage.setOnClickListener {
            if (ProfileStore.currentUrl(this) != null) {
                QrDialogFragment.show(supportFragmentManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        updateNfcWarning()
    }

    private fun shareOneOff() {
        val url = LinkBuilder.custom(oneOffInput.text?.toString().orEmpty())
        if (url == null) {
            oneOffLayout.error = getString(R.string.error_invalid_link)
            return
        }
        oneOffLayout.error = null
        oneOffInput.setText("")
        ProfileStore.setOneOff(this, url)
        hideKeyboard()
        refresh()
    }

    internal fun refresh() {
        val profiles = ProfileStore.profiles(this)
        var activeId = ProfileStore.activeId(this)
        if (profiles.none { it.id == activeId }) {
            // Active profile was deleted (or never set): fall back to the first one
            activeId = profiles.firstOrNull()?.id
            ProfileStore.setActive(this, activeId)
        }
        val oneOff = ProfileStore.oneOffUrl(this)
        adapter.update(profiles, activeId, oneOff != null)
        emptyView.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE

        val active = profiles.find { it.id == activeId }
        val url = oneOff ?: active?.url
        clearOneOff.visibility = if (oneOff != null) View.VISIBLE else View.GONE
        if (url.isNullOrBlank()) {
            qrImage.visibility = View.GONE
            tapHint.visibility = View.GONE
            activeLabel.text = getString(R.string.nothing_shared)
            currentUrl.text = ""
            currentUrl.visibility = View.GONE
        } else {
            qrImage.visibility = View.VISIBLE
            tapHint.visibility = View.VISIBLE
            currentUrl.visibility = View.VISIBLE
            activeLabel.text = if (oneOff != null) getString(R.string.one_off_link) else active!!.name
            currentUrl.text = url
            qrImage.setImageBitmap(QrEncoder.encode(url, 512))
            (qrImage.drawable as? BitmapDrawable)?.setFilterBitmap(false)
        }
    }

    private fun updateNfcWarning() {
        val nfc = NfcAdapter.getDefaultAdapter(this)
        when {
            nfc == null -> {
                nfcWarning.text = getString(R.string.no_nfc)
                nfcWarning.visibility = View.VISIBLE
            }
            !nfc.isEnabled -> {
                nfcWarning.text = getString(R.string.nfc_off)
                nfcWarning.visibility = View.VISIBLE
            }
            else -> nfcWarning.visibility = View.GONE
        }
    }

    private fun showProfileMenu(anchor: View, profile: Profile) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_EDIT, 0, R.string.edit)
            menu.add(0, MENU_DELETE, 1, R.string.delete)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_EDIT -> ProfileDialogFragment.show(supportFragmentManager, profile.id)
                    MENU_DELETE -> DeleteProfileDialogFragment.show(supportFragmentManager, profile.id)
                }
                true
            }
        }.show()
    }

    private fun hideKeyboard() {
        currentFocus?.let { focus ->
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(focus.windowToken, 0)
            focus.clearFocus()
        }
    }

    private companion object {
        const val MENU_EDIT = 1
        const val MENU_DELETE = 2
    }
}
