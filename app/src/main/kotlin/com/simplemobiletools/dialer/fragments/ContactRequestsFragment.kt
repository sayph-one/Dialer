package com.simplemobiletools.dialer.fragments

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.lifecycleScope
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.adapters.ContactRequestsAdapter
import com.simplemobiletools.dialer.database.ContactRequestDatabase
import com.simplemobiletools.dialer.databinding.FragmentContactRequestsBinding
import com.simplemobiletools.dialer.models.ContactRequestListItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ContactRequestsFragment(context: Context, attributeSet: AttributeSet) :
    MyViewPagerFragment<ContactRequestsFragment.ContactRequestsInnerBinding>(context, attributeSet) {

    private lateinit var binding: FragmentContactRequestsBinding
    private lateinit var adapter: ContactRequestsAdapter
    private val database by lazy { ContactRequestDatabase.getDatabase(context) }

    private val expandedSections = mutableSetOf("pending", "approved", "rejected")

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentContactRequestsBinding.bind(this)
        innerBinding = ContactRequestsInnerBinding(binding)
    }

    override fun setupFragment() {
        setupAdapter()
        setupFAB()
        observeRequests()
    }

    override fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int) {
        binding.apply {
            fragmentPlaceholder.setTextColor(textColor)
            fragmentPlaceholder2.setTextColor(textColor)
            fabAddRequest.backgroundTintList = android.content.res.ColorStateList.valueOf(properPrimaryColor)
        }
        adapter.notifyDataSetChanged()
    }

    override fun refreshItems(callback: (() -> Unit)?) {
        // Refresh is handled by Flow observation
        callback?.invoke()
    }

    override fun onSearchClosed() {
        // No search functionality for now
    }

    override fun onSearchQueryChanged(text: String) {
        // No search functionality for now
    }

    private fun setupAdapter() {
        adapter = ContactRequestsAdapter(
            activity = activity as SimpleActivity,
            onHeaderClick = { header ->
                toggleSection(header)
            },
            onRequestClick = { request ->
                // Handle request click (future: show details dialog)
            }
        )
        binding.fragmentList.adapter = adapter
    }

    private fun setupFAB() {
        binding.fabAddRequest.setOnClickListener {
            showAddRequestDialog()
        }
    }

    private fun observeRequests() {
        (activity as? SimpleActivity)?.lifecycleScope?.launch {
            combine(
                database.contactRequestDao().getPendingRequests(),
                database.contactRequestDao().getApprovedRequests(),
                database.contactRequestDao().getRejectedRequests()
            ) { pending, approved, rejected ->
                Triple(pending, approved, rejected)
            }.collect { (pending, approved, rejected) ->
                val listItems = mutableListOf<ContactRequestListItem>()

                // Add pending section
                if (pending.isNotEmpty()) {
                    listItems.add(
                        ContactRequestListItem.Header(
                            title = String.format(context.getString(R.string.pending_requests), pending.size),
                            count = pending.size,
                            isExpanded = expandedSections.contains("pending")
                        )
                    )
                    if (expandedSections.contains("pending")) {
                        listItems.addAll(pending.map { ContactRequestListItem.Request(it) })
                    }
                }

                // Add approved section
                if (approved.isNotEmpty()) {
                    listItems.add(
                        ContactRequestListItem.Header(
                            title = String.format(context.getString(R.string.approved_requests), approved.size),
                            count = approved.size,
                            isExpanded = expandedSections.contains("approved")
                        )
                    )
                    if (expandedSections.contains("approved")) {
                        listItems.addAll(approved.map { ContactRequestListItem.Request(it) })
                    }
                }

                // Add rejected section
                if (rejected.isNotEmpty()) {
                    listItems.add(
                        ContactRequestListItem.Header(
                            title = String.format(context.getString(R.string.rejected_requests), rejected.size),
                            count = rejected.size,
                            isExpanded = expandedSections.contains("rejected")
                        )
                    )
                    if (expandedSections.contains("rejected")) {
                        listItems.addAll(rejected.map { ContactRequestListItem.Request(it) })
                    }
                }

                adapter.submitList(listItems)

                // Show/hide placeholder
                if (listItems.isEmpty()) {
                    binding.fragmentPlaceholder.beVisible()
                    binding.fragmentPlaceholder2.beVisible()
                    binding.fragmentList.beGone()
                } else {
                    binding.fragmentPlaceholder.beGone()
                    binding.fragmentPlaceholder2.beGone()
                    binding.fragmentList.beVisible()
                }
            }
        }
    }

    private fun toggleSection(header: ContactRequestListItem.Header) {
        val sectionKey = when {
            header.title.contains("Pending") -> "pending"
            header.title.contains("Approved") -> "approved"
            header.title.contains("Rejected") -> "rejected"
            else -> return
        }

        if (expandedSections.contains(sectionKey)) {
            expandedSections.remove(sectionKey)
        } else {
            expandedSections.add(sectionKey)
        }

        // Trigger refresh by observing again
        observeRequests()
    }

    private fun showAddRequestDialog() {
        AddContactRequestDialog(activity as SimpleActivity) { firstName, lastName, phone ->
            (activity as? SimpleActivity)?.lifecycleScope?.launch {
                val request = com.simplemobiletools.dialer.database.ContactRequestEntity(
                    requestId = java.util.UUID.randomUUID().toString(),
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    requestSource = "user",
                    requestedAt = System.currentTimeMillis(),
                    syncedToAgent = false,
                    status = "pending"
                )
                database.contactRequestDao().insert(request)
            }
        }
    }

    class ContactRequestsInnerBinding(val binding: FragmentContactRequestsBinding) : InnerBinding {
        override val fragmentList: MyRecyclerView = binding.fragmentList
        override val recentsList = null
    }
}
