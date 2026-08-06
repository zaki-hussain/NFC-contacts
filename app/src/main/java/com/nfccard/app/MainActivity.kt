package com.nfccard.app

import android.graphics.drawable.BitmapDrawable
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID

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
        oneOffInput.setOnEditorActionListener { _, _, _ -> shareOneOff(); true }
        clearOneOff.setOnClickListener {
            ProfileStore.clearOneOff(this)
            refresh()
        }
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { showProfileDialog(null) }
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

    private fun refresh() {
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
                    MENU_EDIT -> showProfileDialog(profile)
                    MENU_DELETE -> confirmDelete(profile)
                }
                true
            }
        }.show()
    }

    private fun confirmDelete(profile: Profile) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_profile_title, profile.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                ProfileStore.saveProfiles(this, ProfileStore.profiles(this).filter { it.id != profile.id })
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showProfileDialog(existing: Profile?) {
        val view = layoutInflater.inflate(R.layout.dialog_profile, null)
        val chips = view.findViewById<ChipGroup>(R.id.kind_chips)
        val nameInput = view.findViewById<TextInputEditText>(R.id.name_input)
        val linkLayout = view.findViewById<TextInputLayout>(R.id.link_layout)
        val linkInput = view.findViewById<TextInputEditText>(R.id.link_input)

        fun kind(): ProfileKind = when (chips.checkedChipId) {
            R.id.chip_linkedin -> ProfileKind.LINKEDIN
            R.id.chip_whatsapp -> ProfileKind.WHATSAPP
            else -> ProfileKind.CUSTOM
        }

        fun defaultName(kind: ProfileKind): String? = when (kind) {
            ProfileKind.LINKEDIN -> getString(R.string.linkedin)
            ProfileKind.WHATSAPP -> getString(R.string.whatsapp)
            ProfileKind.CUSTOM -> null
        }

        fun applyKind() {
            val k = kind()
            linkLayout.hint = getString(
                when (k) {
                    ProfileKind.LINKEDIN -> R.string.hint_linkedin
                    ProfileKind.WHATSAPP -> R.string.hint_whatsapp
                    ProfileKind.CUSTOM -> R.string.hint_custom
                }
            )
            linkLayout.error = null
            // Auto-fill the name unless the user typed their own
            val current = nameInput.text?.toString().orEmpty()
            val defaults = listOf(getString(R.string.linkedin), getString(R.string.whatsapp))
            if (current.isEmpty() || current in defaults) {
                nameInput.setText(defaultName(k).orEmpty())
            }
        }

        when (existing?.kind) {
            ProfileKind.WHATSAPP -> chips.check(R.id.chip_whatsapp)
            ProfileKind.CUSTOM -> chips.check(R.id.chip_custom)
            else -> chips.check(R.id.chip_linkedin)
        }
        applyKind()
        if (existing != null) {
            nameInput.setText(existing.name)
            linkInput.setText(existing.url)
        }
        chips.setOnCheckedStateChangeListener { _, _ -> applyKind() }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_profile else R.string.edit_profile)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val k = kind()
                val input = linkInput.text?.toString().orEmpty()
                val url = when (k) {
                    ProfileKind.LINKEDIN -> LinkBuilder.linkedIn(input)
                    ProfileKind.WHATSAPP -> LinkBuilder.whatsApp(input)
                    ProfileKind.CUSTOM -> LinkBuilder.custom(input)
                }
                if (url == null) {
                    linkLayout.error = getString(
                        when (k) {
                            ProfileKind.LINKEDIN -> R.string.error_invalid_linkedin
                            ProfileKind.WHATSAPP -> R.string.error_invalid_phone
                            ProfileKind.CUSTOM -> R.string.error_invalid_link
                        }
                    )
                    return@setOnClickListener
                }
                val name = nameInput.text?.toString()?.trim().orEmpty()
                    .ifEmpty { defaultName(k) ?: url }
                val profiles = ProfileStore.profiles(this).toMutableList()
                if (existing == null) {
                    val profile = Profile(UUID.randomUUID().toString(), name, url, k)
                    profiles.add(profile)
                    ProfileStore.saveProfiles(this, profiles)
                    if (ProfileStore.currentUrl(this) == null) {
                        ProfileStore.selectProfile(this, profile.id)
                    }
                } else {
                    val updated = profiles.map {
                        if (it.id == existing.id) it.copy(name = name, url = url, kind = k) else it
                    }
                    ProfileStore.saveProfiles(this, updated)
                }
                refresh()
                dialog.dismiss()
            }
        }
        dialog.show()
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
