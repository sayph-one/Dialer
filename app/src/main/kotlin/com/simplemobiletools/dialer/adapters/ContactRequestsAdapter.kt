package com.simplemobiletools.dialer.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.dialer.R
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

                // Format time ago
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    entity.requestedAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                requestTime.text = activity.getString(R.string.requested_time_ago, timeAgo)

                // Hide status badge since segments already indicate status
                statusBadge.visibility = View.GONE

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
