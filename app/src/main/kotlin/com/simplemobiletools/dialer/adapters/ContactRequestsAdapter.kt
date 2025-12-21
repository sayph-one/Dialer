package com.simplemobiletools.dialer.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.databinding.ItemContactRequestBinding
import com.simplemobiletools.dialer.models.ContactRequestListItem

class ContactRequestsAdapter(
    private val activity: SimpleActivity,
    private val onRequestClick: (ContactRequestListItem.Request) -> Unit
) : ListAdapter<ContactRequestListItem, ContactRequestsAdapter.RequestViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemContactRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val item = getItem(position)
        if (item is ContactRequestListItem.Request) {
            holder.bind(item)
        }
    }

    inner class RequestViewHolder(private val binding: ItemContactRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: ContactRequestListItem.Request) {
            val entity = request.entity
            binding.apply {
                // Combine firstName and lastName
                val fullName = if (entity.lastName.isNotEmpty()) {
                    "${entity.firstName} ${entity.lastName}"
                } else {
                    entity.firstName
                }
                contactName.text = fullName
                contactPhone.text = entity.phone

                // Set contact initial (first letter of name)
                contactInitial.text = entity.firstName.firstOrNull()?.uppercase() ?: "?"

                // Set avatar background color based on status
                val avatarColor = when (entity.status) {
                    "pending" -> activity.getProperPrimaryColor() // Blue (app's primary color)
                    "approved" -> Color.parseColor("#A3D78A")      // Green
                    "rejected" -> Color.parseColor("#FE5757")      // Red
                    else -> activity.getProperPrimaryColor()
                }
                avatarContainer.backgroundTintList = ColorStateList.valueOf(avatarColor)

                // Format time ago (short format)
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    entity.requestedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                requestTime.text = timeAgo

                // Apply text colors
                contactName.setTextColor(activity.getProperTextColor())
                contactPhone.setTextColor(activity.getProperTextColor())
                requestTime.setTextColor(activity.getProperTextColor())

                root.setOnClickListener {
                    onRequestClick(request)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ContactRequestListItem>() {
        override fun areItemsTheSame(
            oldItem: ContactRequestListItem,
            newItem: ContactRequestListItem
        ): Boolean {
            return when {
                oldItem is ContactRequestListItem.Request && newItem is ContactRequestListItem.Request ->
                    oldItem.entity.requestId == newItem.entity.requestId
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: ContactRequestListItem,
            newItem: ContactRequestListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
