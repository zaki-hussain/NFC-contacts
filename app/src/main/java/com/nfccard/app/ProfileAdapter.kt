package com.nfccard.app

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

class ProfileAdapter(
    private val onSelect: (Profile) -> Unit,
    private val onMenu: (View, Profile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.Holder>() {

    private var items: List<Profile> = emptyList()
    private var activeId: String? = null
    private var oneOffActive = false

    fun update(profiles: List<Profile>, activeId: String?, oneOffActive: Boolean) {
        items = profiles
        this.activeId = activeId
        this.oneOffActive = oneOffActive
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: TextView = view.findViewById(R.id.name)
        val url: TextView = view.findViewById(R.id.url)
        val radio: RadioButton = view.findViewById(R.id.radio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val profile = items[position]
        holder.name.text = profile.name
        holder.url.text = profile.url
        when (profile.kind) {
            ProfileKind.LINKEDIN -> {
                holder.icon.setImageResource(R.drawable.ic_linkedin)
                holder.icon.imageTintList = null
            }
            ProfileKind.WHATSAPP -> {
                holder.icon.setImageResource(R.drawable.ic_whatsapp)
                holder.icon.imageTintList = null
            }
            ProfileKind.CUSTOM -> {
                holder.icon.setImageResource(R.drawable.ic_link)
                holder.icon.imageTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        holder.icon,
                        com.google.android.material.R.attr.colorPrimary
                    )
                )
            }
        }
        holder.radio.isChecked = !oneOffActive && profile.id == activeId
        holder.itemView.setOnClickListener { onSelect(profile) }
        holder.itemView.setOnLongClickListener {
            onMenu(it, profile)
            true
        }
    }
}
