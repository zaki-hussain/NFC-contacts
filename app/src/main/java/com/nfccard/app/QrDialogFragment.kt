package com.nfccard.app

import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/** Fullscreen enlarged QR of the current link. Tap outside the code or the X to close. */
class QrDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_NFCCard_QrDialog)
        val bitmap = ProfileStore.currentShare(requireContext())
            ?.let { runCatching { QrEncoder.encode(it.payload, 1024) }.getOrNull() }
        if (bitmap == null) {
            // Share disappeared (e.g. profile deleted); nothing to show
            dismiss()
            return dialog
        }
        val view = layoutInflater.inflate(R.layout.dialog_qr, null)
        val qr = view.findViewById<ImageView>(R.id.qr_large)
        qr.setImageBitmap(bitmap)
        (qr.drawable as? BitmapDrawable)?.setFilterBitmap(false)

        view.setOnClickListener { dismiss() }
        // The card consumes its own clicks (android:clickable) so tapping the QR keeps it open
        view.findViewById<View>(R.id.close).setOnClickListener { dismiss() }

        dialog.setContentView(view)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return dialog
    }

    companion object {
        fun show(fm: FragmentManager) {
            QrDialogFragment().show(fm, "qr_dialog")
        }
    }
}
