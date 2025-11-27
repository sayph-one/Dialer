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
import com.simplemobiletools.dialer.databinding.ItemContactRequestHeaderBinding
import com.simplemobiletools.dialer.models.ContactRequestListItem

class ContactRequestsAdapter(
    private val activity: SimpleActivity,
    private val onHeaderClick: (ContactRequestListItem.Header) -> Unit,
    private val onRequestClick: (ContactRequestListItem.Request) -> Unit
) : ListAdapter<ContactRequestListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_REQUEST = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactRequestListItem.Header -> TYPE_HEADER
            is ContactRequestListItem.Request -> TYPE_REQUEST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemContactRequestHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_REQUEST -> {
                val binding = ItemContactRequestBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                RequestViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactRequestListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactRequestListItem.Request -> (holder as RequestViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemContactRequestHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: ContactRequestListItem.Header) {
            binding.apply {
                sectionTitle.text = header.title
                sectionTitle.setTextColor(activity.getProperTextColor())

                // Rotate arrow based on expanded state
                sectionArrow.rotation = if (header.isExpanded) 180f else 0f

                root.setOnClickListener {
                    onHeaderClick(header)
                }
            }
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

                // Show status badge for rejected requests
                when (entity.status) {
                    "rejected" -> {
                        statusBadge.visibility = View.VISIBLE
                        statusBadge.text = activity.getString(R.string.request_status_rejected)
                        statusBadge.setTextColor(
                            ContextCompat.getColor(activity, R.color.md_red_700)
                        )
                    }
                    "approved" -> {
                        statusBadge.visibility = View.VISIBLE
                        statusBadge.text = activity.getString(R.string.request_status_approved)
                        statusBadge.setTextColor(
                            ContextCompat.getColor(activity, R.color.md_green_700)
                        )
                    }
                    else -> {
                        statusBadge.visibility = View.GONE
                    }
                }

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
                oldItem is ContactRequestListItem.Header && newItem is ContactRequestListItem.Header ->
                    oldItem.title == newItem.title
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
