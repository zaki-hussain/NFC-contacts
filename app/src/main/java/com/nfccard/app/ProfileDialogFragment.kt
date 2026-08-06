package com.nfccard.app

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.UUID

/** Add/edit profile dialog. A DialogFragment so typed input survives rotation. */
class ProfileDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val existing = arguments?.getString(ARG_ID)?.let { id ->
            ProfileStore.profiles(requireContext()).find { it.id == id }
        }
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
                val context = requireContext()
                val name = nameInput.text?.toString()?.trim().orEmpty()
                    .ifEmpty { defaultName(k) ?: url }
                val profiles = ProfileStore.profiles(context).toMutableList()
                if (existing == null) {
                    val profile = Profile(UUID.randomUUID().toString(), name, url, k)
                    profiles.add(profile)
                    ProfileStore.saveProfiles(context, profiles)
                    if (ProfileStore.currentUrl(context) == null) {
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

    companion object {
        private const val ARG_ID = "profile_id"

        fun show(fm: FragmentManager, profileId: String?) {
            ProfileDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_ID, profileId) }
            }.show(fm, "profile_dialog")
        }
    }
}
