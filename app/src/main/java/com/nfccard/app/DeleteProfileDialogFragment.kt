package com.nfccard.app

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Delete-profile confirmation. A DialogFragment so it survives rotation. */
class DeleteProfileDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val id = requireArguments().getString(ARG_ID).orEmpty()
        val context = requireContext()
        val name = ProfileStore.profiles(context).find { it.id == id }?.name.orEmpty()
        return MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.delete_profile_title, name))
            .setPositiveButton(R.string.delete) { _, _ ->
                ProfileStore.saveProfiles(
                    context,
                    ProfileStore.profiles(context).filter { it.id != id }
                )
                (activity as? MainActivity)?.refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        private const val ARG_ID = "profile_id"

        fun show(fm: FragmentManager, profileId: String) {
            DeleteProfileDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_ID, profileId) }
            }.show(fm, "delete_dialog")
        }
    }
}
