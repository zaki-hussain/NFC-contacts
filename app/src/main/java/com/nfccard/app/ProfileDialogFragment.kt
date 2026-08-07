package com.nfccard.app

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID

/** Add/edit profile dialog. A DialogFragment so typed input survives rotation. */
class ProfileDialogFragment : DialogFragment() {

    private var vcardText: String? = null
    private var nameInput: TextInputEditText? = null
    private var vcardStatus: TextView? = null

    private val pickVcard =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) loadVcard(uri)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val existing = arguments?.getString(ARG_ID)?.let { id ->
            ProfileStore.profiles(requireContext()).find { it.id == id }
        }
        vcardText = savedInstanceState?.getString(STATE_VCARD)
            ?: existing?.takeIf { it.kind == ProfileKind.VCARD }?.url

        val view = layoutInflater.inflate(R.layout.dialog_profile, null)
        val chips = view.findViewById<ChipGroup>(R.id.kind_chips)
        val nameInput = view.findViewById<TextInputEditText>(R.id.name_input)
        val linkLayout = view.findViewById<TextInputLayout>(R.id.link_layout)
        val linkInput = view.findViewById<TextInputEditText>(R.id.link_input)
        val vcardRow = view.findViewById<View>(R.id.vcard_row)
        this.nameInput = nameInput
        this.vcardStatus = view.findViewById(R.id.vcard_status)
        view.findViewById<View>(R.id.attach_vcard).setOnClickListener {
            pickVcard.launch(arrayOf("*/*"))
        }
        updateVcardStatus()

        fun kind(): ProfileKind = when (chips.checkedChipId) {
            R.id.chip_linkedin -> ProfileKind.LINKEDIN
            R.id.chip_whatsapp -> ProfileKind.WHATSAPP
            R.id.chip_contact -> ProfileKind.VCARD
            else -> ProfileKind.CUSTOM
        }

        fun defaultName(kind: ProfileKind): String? = when (kind) {
            ProfileKind.LINKEDIN -> getString(R.string.linkedin)
            ProfileKind.WHATSAPP -> getString(R.string.whatsapp)
            ProfileKind.VCARD -> vcardText?.let { VCard.displayName(it) }
            ProfileKind.CUSTOM -> null
        }

        fun applyKind() {
            val k = kind()
            val isContact = k == ProfileKind.VCARD
            linkLayout.visibility = if (isContact) View.GONE else View.VISIBLE
            vcardRow.visibility = if (isContact) View.VISIBLE else View.GONE
            if (!isContact) {
                linkLayout.hint = getString(
                    when (k) {
                        ProfileKind.LINKEDIN -> R.string.hint_linkedin
                        ProfileKind.WHATSAPP -> R.string.hint_whatsapp
                        else -> R.string.hint_custom
                    }
                )
                linkLayout.error = null
            }
            // Auto-fill the name unless the user typed their own
            val current = nameInput.text?.toString().orEmpty()
            val defaults = listOf(getString(R.string.linkedin), getString(R.string.whatsapp))
            if (current.isEmpty() || current in defaults) {
                nameInput.setText(defaultName(k).orEmpty())
            }
        }

        when (existing?.kind) {
            ProfileKind.LINKEDIN -> chips.check(R.id.chip_linkedin)
            ProfileKind.WHATSAPP -> chips.check(R.id.chip_whatsapp)
            ProfileKind.CUSTOM -> chips.check(R.id.chip_custom)
            ProfileKind.VCARD, null -> chips.check(R.id.chip_contact)
        }
        applyKind()
        if (existing != null) {
            nameInput.setText(existing.name)
            if (existing.kind != ProfileKind.VCARD) linkInput.setText(existing.url)
        }
        chips.setOnCheckedStateChangeListener { _, _ -> applyKind() }

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
                    ProfileKind.VCARD -> vcardText
                }
                if (url == null) {
                    if (k == ProfileKind.VCARD) {
                        updateVcardStatus(getString(R.string.error_no_vcard))
                    } else {
                        linkLayout.error = getString(
                            when (k) {
                                ProfileKind.LINKEDIN -> R.string.error_invalid_linkedin
                                ProfileKind.WHATSAPP -> R.string.error_invalid_phone
                                else -> R.string.error_invalid_link
                            }
                        )
                    }
                    return@setOnClickListener
                }
                val context = requireContext()
                val name = nameInput.text?.toString()?.trim().orEmpty()
                    .ifEmpty {
                        defaultName(k)
                            ?: if (k == ProfileKind.VCARD) getString(R.string.contact_card) else url
                    }
                val profiles = ProfileStore.profiles(context).toMutableList()
                if (existing == null) {
                    val profile = Profile(UUID.randomUUID().toString(), name, url, k)
                    profiles.add(profile)
                    ProfileStore.saveProfiles(context, profiles)
                    if (ProfileStore.currentShare(context) == null) {
                        ProfileStore.selectProfile(context, profile.id)
                    }
                } else {
                    val updated = profiles.map {
                        if (it.id == existing.id) it.copy(name = name, url = url, kind = k) else it
                    }
                    ProfileStore.saveProfiles(context, updated)
                }
                (activity as? MainActivity)?.refresh()
                dismiss()
            }
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_VCARD, vcardText)
    }

    private fun loadVcard(uri: Uri) {
        val text = runCatching {
            requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        val clean = text?.let { VCard.sanitize(it) }
        when {
            clean == null ->
                updateVcardStatus(getString(R.string.error_bad_vcard))
            clean.toByteArray(Charsets.UTF_8).size > MAX_VCARD_BYTES ->
                updateVcardStatus(getString(R.string.error_vcard_too_big))
            else -> {
                vcardText = clean
                // Auto-fill the name from the card unless the user typed their own
                val fn = VCard.displayName(clean)
                val current = nameInput?.text?.toString().orEmpty()
                val defaults = listOf(getString(R.string.linkedin), getString(R.string.whatsapp))
                if (fn != null && (current.isEmpty() || current in defaults)) {
                    nameInput?.setText(fn)
                }
                updateVcardStatus()
            }
        }
    }

    private fun updateVcardStatus(error: String? = null) {
        val status = vcardStatus ?: return
        if (error != null) {
            status.text = error
            status.setTextColor(
                MaterialColors.getColor(status, com.google.android.material.R.attr.colorError)
            )
        } else {
            status.text = vcardText
                ?.let { getString(R.string.vcard_loaded, VCard.displayName(it) ?: getString(R.string.contact_card)) }
                ?: getString(R.string.vcard_none)
            status.setTextColor(
                MaterialColors.getColor(status, com.google.android.material.R.attr.colorOnSurfaceVariant)
            )
        }
    }

    companion object {
        private const val ARG_ID = "profile_id"
        private const val STATE_VCARD = "vcard_text"

        /** Fits comfortably in the tag's 4KB NDEF file and within QR capacity. */
        private const val MAX_VCARD_BYTES = 2500

        fun show(fm: FragmentManager, profileId: String?) {
            ProfileDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_ID, profileId) }
            }.show(fm, "profile_dialog")
        }
    }
}
