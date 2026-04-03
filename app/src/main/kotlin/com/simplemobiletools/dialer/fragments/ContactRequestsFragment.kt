package com.simplemobiletools.dialer.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.adapters.ContactRequestsAdapter
import com.simplemobiletools.dialer.database.ContactRequestDatabase
import com.simplemobiletools.dialer.database.ContactRequestEntity
import com.simplemobiletools.dialer.databinding.FragmentContactRequestsBinding
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.helpers.DemoDataProvider
import com.simplemobiletools.dialer.models.ContactRequestListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ContactRequestsFragment(context: Context, attributeSet: AttributeSet) :
    MyViewPagerFragment<ContactRequestsFragment.ContactRequestsInnerBinding>(context, attributeSet) {

    private lateinit var binding: FragmentContactRequestsBinding
    private lateinit var adapter: ContactRequestsAdapter
    private val database by lazy { ContactRequestDatabase.getDatabase(context) }

    private var currentSegment = Segment.PENDING
    private var searchQuery = ""
    private var isSearchActive = false
    private var collectionJob: Job? = null

    private enum class Segment {
        PENDING, APPROVED, REJECTED
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentContactRequestsBinding.bind(this)
        innerBinding = ContactRequestsInnerBinding(binding)
    }

    override fun setupFragment() {
        setupAdapter()
        setupSegmentedButtons()
        setupFAB()
        observeRequests()
    }

    override fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int) {
        binding.apply {
            fragmentPlaceholder.setTextColor(textColor)
            fragmentPlaceholder2.setTextColor(textColor)
            fabAddRequest.backgroundTintList = ColorStateList.valueOf(properPrimaryColor)

            // Style the segmented buttons
            val buttonStrokeColor = ColorStateList.valueOf(properPrimaryColor)
            btnPending.strokeColor = buttonStrokeColor
            btnApproved.strokeColor = buttonStrokeColor
            btnRejected.strokeColor = buttonStrokeColor

            // Set ripple and checked colors
            val checkedColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(properPrimaryColor, android.graphics.Color.TRANSPARENT)
            )
            btnPending.backgroundTintList = checkedColor
            btnApproved.backgroundTintList = checkedColor
            btnRejected.backgroundTintList = checkedColor

            // Text colors
            val textColors = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(android.graphics.Color.WHITE, textColor)
            )
            btnPending.setTextColor(textColors)
            btnApproved.setTextColor(textColors)
            btnRejected.setTextColor(textColors)
        }
        adapter.notifyDataSetChanged()
    }

    override fun refreshItems(callback: (() -> Unit)?) {
        // Refresh is handled by Flow observation
        callback?.invoke()
    }

    override fun onSearchClosed() {
        isSearchActive = false
        searchQuery = ""
        binding.segmentToggleGroup.beVisible()
        observeRequests()
    }

    override fun onSearchQueryChanged(text: String) {
        searchQuery = text
        if (text.isNotEmpty()) {
            isSearchActive = true
            binding.segmentToggleGroup.beGone()
            searchAllRequests(text)
        } else {
            isSearchActive = false
            binding.segmentToggleGroup.beVisible()
            observeRequests()
        }
    }

    private fun setupAdapter() {
        adapter = ContactRequestsAdapter(
            activity = activity as SimpleActivity,
            onRequestClick = { request ->
                // Handle request click (future: show details dialog)
            }
        )
        binding.fragmentList.adapter = adapter
    }

    private fun setupSegmentedButtons() {
        binding.segmentToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentSegment = when (checkedId) {
                    R.id.btn_pending -> Segment.PENDING
                    R.id.btn_approved -> Segment.APPROVED
                    R.id.btn_rejected -> Segment.REJECTED
                    else -> Segment.PENDING
                }
                observeRequests()
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddRequest.setOnClickListener {
            showAddRequestDialog()
        }
    }

    private fun observeRequests() {
        collectionJob?.cancel()

        // Demo mode: show only demo data (no database access)
        if (context.config.demoMode) {
            val status = when (currentSegment) {
                Segment.PENDING -> "pending"
                Segment.APPROVED -> "approved"
                Segment.REJECTED -> "rejected"
            }
            val allRequests = DemoDataProvider.getAllDemoRequests(status)
            val listItems = allRequests.map { ContactRequestListItem.Request(it) }
            adapter.submitList(listItems)
            updatePlaceholderVisibility(listItems.isEmpty())
            return
        }

        collectionJob = (activity as? SimpleActivity)?.lifecycleScope?.launch {
            val flow: Flow<List<ContactRequestEntity>> = when (currentSegment) {
                Segment.PENDING -> database.contactRequestDao().getPendingRequests()
                Segment.APPROVED -> database.contactRequestDao().getApprovedRequests()
                Segment.REJECTED -> database.contactRequestDao().getRejectedRequests()
            }

            flow.collect { requests ->
                // Don't update if search is active
                if (isSearchActive) return@collect

                val listItems = requests.map { ContactRequestListItem.Request(it) }
                adapter.submitList(listItems)

                // Show/hide placeholder based on current segment
                if (listItems.isEmpty()) {
                    binding.fragmentPlaceholder.beVisible()
                    binding.fragmentPlaceholder2.beVisible()
                    binding.fragmentList.beGone()

                    // Update placeholder text based on segment
                    binding.fragmentPlaceholder.text = when (currentSegment) {
                        Segment.PENDING -> context.getString(R.string.no_pending_requests)
                        Segment.APPROVED -> context.getString(R.string.no_approved_requests)
                        Segment.REJECTED -> context.getString(R.string.no_rejected_requests)
                    }
                    binding.fragmentPlaceholder2.text = if (currentSegment == Segment.PENDING) {
                        context.getString(R.string.tap_plus_to_request)
                    } else {
                        ""
                    }
                } else {
                    binding.fragmentPlaceholder.beGone()
                    binding.fragmentPlaceholder2.beGone()
                    binding.fragmentList.beVisible()
                }
            }
        }
    }

    private fun searchAllRequests(query: String) {
        collectionJob?.cancel()
        collectionJob = (activity as? SimpleActivity)?.lifecycleScope?.launch {
            // Combine all request types into one flow
            combine(
                database.contactRequestDao().getPendingRequests(),
                database.contactRequestDao().getApprovedRequests(),
                database.contactRequestDao().getRejectedRequests()
            ) { pending, approved, rejected ->
                // Combine all requests
                pending + approved + rejected
            }.collect { allRequests ->
                // Filter by search query
                val filteredRequests = allRequests.filter { request ->
                    val fullName = "${request.firstName} ${request.lastName}".trim()
                    fullName.contains(query, ignoreCase = true) ||
                    request.phone.contains(query, ignoreCase = true)
                }

                val listItems = filteredRequests.map { ContactRequestListItem.Request(it) }
                adapter.submitList(listItems)

                // Show/hide placeholder
                if (listItems.isEmpty()) {
                    binding.fragmentPlaceholder.beVisible()
                    binding.fragmentPlaceholder2.beGone()
                    binding.fragmentList.beGone()
                    binding.fragmentPlaceholder.text = context.getString(R.string.no_contact_requests_found)
                } else {
                    binding.fragmentPlaceholder.beGone()
                    binding.fragmentPlaceholder2.beGone()
                    binding.fragmentList.beVisible()
                }
            }
        }
    }

    private fun updatePlaceholderVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            binding.fragmentPlaceholder.beVisible()
            binding.fragmentPlaceholder2.beVisible()
            binding.fragmentList.beGone()

            binding.fragmentPlaceholder.text = when (currentSegment) {
                Segment.PENDING -> context.getString(R.string.no_pending_requests)
                Segment.APPROVED -> context.getString(R.string.no_approved_requests)
                Segment.REJECTED -> context.getString(R.string.no_rejected_requests)
            }
            binding.fragmentPlaceholder2.text = if (currentSegment == Segment.PENDING) {
                context.getString(R.string.tap_plus_to_request)
            } else {
                ""
            }
        } else {
            binding.fragmentPlaceholder.beGone()
            binding.fragmentPlaceholder2.beGone()
            binding.fragmentList.beVisible()
        }
    }

    private fun showAddRequestDialog() {
        AddContactRequestDialog(activity as SimpleActivity) { firstName, lastName, phone ->
            val request = ContactRequestEntity(
                requestId = java.util.UUID.randomUUID().toString(),
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                requestSource = "user",
                requestedAt = System.currentTimeMillis(),
                syncedToAgent = false,
                status = "pending"
            )

            if (context.config.demoMode) {
                // In demo mode, add to session storage (in-memory only)
                DemoDataProvider.addSessionRequest(request)
                // Refresh the list to show the new request
                observeRequests()
            } else {
                // Normal mode: save to database
                (activity as? SimpleActivity)?.lifecycleScope?.launch {
                    database.contactRequestDao().insert(request)
                }
            }
        }
    }

    class ContactRequestsInnerBinding(val binding: FragmentContactRequestsBinding) : InnerBinding {
        override val fragmentList: MyRecyclerView = binding.fragmentList
        override val recentsList = null
    }
}
