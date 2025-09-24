package com.simplemobiletools.dialer.fragments

import android.content.Context
import android.provider.ContactsContract
import android.util.AttributeSet
import com.simplemobiletools.commons.dialogs.CallConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.MyContactsContentProvider
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CALL_LOG
import com.simplemobiletools.commons.helpers.SMT_PRIVATE
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.adapters.RecentCallsAdapter
import com.simplemobiletools.dialer.databinding.FragmentRecentsBinding
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.helpers.ContactFiltering
import com.simplemobiletools.dialer.helpers.MIN_RECENTS_THRESHOLD
import com.simplemobiletools.dialer.helpers.RecentsHelper
import com.simplemobiletools.dialer.interfaces.RefreshItemsListener
import com.simplemobiletools.dialer.models.RecentCall

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.RecentsInnerBinding>(context, attributeSet),
    RefreshItemsListener {
    private lateinit var binding: FragmentRecentsBinding
    private var allRecentCalls = listOf<RecentCall>()
    private var recentsAdapter: RecentCallsAdapter? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentRecentsBinding.bind(this)
        innerBinding = RecentsInnerBinding(binding)
    }

    override fun setupFragment() {
        val placeholderResId = if (context.hasPermission(PERMISSION_READ_CALL_LOG)) {
            R.string.no_previous_calls
        } else {
            R.string.could_not_access_the_call_history
        }

        binding.recentsPlaceholder.text = context.getString(placeholderResId)
        binding.recentsPlaceholder2.apply {
            underlineText()
            setOnClickListener {
                requestCallLogPermission()
            }
        }
    }

    override fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int) {
        binding.recentsPlaceholder.setTextColor(textColor)
        binding.recentsPlaceholder2.setTextColor(properPrimaryColor)

        recentsAdapter?.apply {
            initDrawables()
            updateTextColor(textColor)
        }
    }

    override fun refreshItems(callback: (() -> Unit)?) {
        val privateCursor = context?.getMyContactsCursor(false, true)
        val groupSubsequentCalls = context?.config?.groupSubsequentCalls ?: false
        val querySize = allRecentCalls.size.coerceAtLeast(MIN_RECENTS_THRESHOLD)
        RecentsHelper(context).getRecentCalls(groupSubsequentCalls, querySize) { recents ->
            ContactsHelper(context).getContacts(showOnlyContactsWithNumbers = true) { contacts ->
                // Filter contacts to exclude SIM contacts
                val filteredContacts = ContactFiltering.filterDeviceContacts(context, contacts)

                val privateContacts = MyContactsContentProvider.getContacts(context, privateCursor)
                // Also filter private contacts
                val filteredPrivateContacts = ContactFiltering.filterDeviceContacts(context, privateContacts)

                allRecentCalls = recents
                    .onlyFromKnownContacts(context, filteredContacts, filteredPrivateContacts)
                    .setNamesIfEmpty(filteredContacts, filteredPrivateContacts)
                    .hidePrivateContacts(filteredPrivateContacts, SMT_PRIVATE in context.baseConfig.ignoredContactSources)

                activity?.runOnUiThread {
                    gotRecents(allRecentCalls)
                }
            }
        }
    }

    private fun gotRecents(recents: List<RecentCall>) {
        if (recents.isEmpty()) {
            binding.apply {
                recentsPlaceholder.beVisible()
                recentsPlaceholder2.beGoneIf(context.hasPermission(PERMISSION_READ_CALL_LOG))
                recentsList.beGone()
            }
        } else {
            binding.apply {
                recentsPlaceholder.beGone()
                recentsPlaceholder2.beGone()
                recentsList.beVisible()
            }

            if (binding.recentsList.adapter == null) {
                recentsAdapter = RecentCallsAdapter(activity as SimpleActivity, recents.toMutableList(), binding.recentsList, this, true) {
                    val recentCall = it as RecentCall
                    if (context.config.showCallConfirmation) {
                        CallConfirmationDialog(activity as SimpleActivity, recentCall.name) {
                            activity?.launchCallIntent(recentCall.phoneNumber)
                        }
                    } else {
                        activity?.launchCallIntent(recentCall.phoneNumber)
                    }
                }

                binding.recentsList.adapter = recentsAdapter

                if (context.areSystemAnimationsEnabled) {
                    binding.recentsList.scheduleLayoutAnimation()
                }

                binding.recentsList.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {}

                    override fun updateBottom() {
                        getMoreRecentCalls()
                    }
                }

            } else {
                recentsAdapter?.updateItems(recents)
            }
        }
    }

    private fun getMoreRecentCalls() {
        val privateCursor = context?.getMyContactsCursor(false, true)
        val groupSubsequentCalls = context?.config?.groupSubsequentCalls ?: false
        val querySize = allRecentCalls.size.plus(MIN_RECENTS_THRESHOLD)
        RecentsHelper(context).getRecentCalls(groupSubsequentCalls, querySize) { recents ->
            ContactsHelper(context).getContacts(showOnlyContactsWithNumbers = true) { contacts ->
                // Filter contacts to exclude SIM contacts
                val filteredContacts = ContactFiltering.filterDeviceContacts(context, contacts)

                val privateContacts = MyContactsContentProvider.getContacts(context, privateCursor)
                // Also filter private contacts
                val filteredPrivateContacts = ContactFiltering.filterDeviceContacts(context, privateContacts)

                allRecentCalls = recents
                    .onlyFromKnownContacts(context, filteredContacts, filteredPrivateContacts)
                    .setNamesIfEmpty(filteredContacts, filteredPrivateContacts)
                    .hidePrivateContacts(filteredPrivateContacts, SMT_PRIVATE in context.baseConfig.ignoredContactSources)

                activity?.runOnUiThread {
                    gotRecents(allRecentCalls)
                }
            }
        }
    }

    private fun requestCallLogPermission() {
        activity?.handlePermission(PERMISSION_READ_CALL_LOG) {
            if (it) {
                binding.recentsPlaceholder.text = context.getString(R.string.no_previous_calls)
                binding.recentsPlaceholder2.beGone()

                val groupSubsequentCalls = context?.config?.groupSubsequentCalls ?: false
                RecentsHelper(context).getRecentCalls(groupSubsequentCalls) { recents ->
                    activity?.runOnUiThread {
                        gotRecents(recents)
                    }
                }
            }
        }
    }

    override fun onSearchClosed() {
        binding.recentsPlaceholder.beVisibleIf(allRecentCalls.isEmpty())
        recentsAdapter?.updateItems(allRecentCalls)
    }

    override fun onSearchQueryChanged(text: String) {
        val recentCalls = allRecentCalls.filter {
            it.name.contains(text, true) || it.doesContainPhoneNumber(text)
        }.sortedByDescending {
            it.name.startsWith(text, true)
        }.toMutableList() as ArrayList<RecentCall>

        binding.recentsPlaceholder.beVisibleIf(recentCalls.isEmpty())
        recentsAdapter?.updateItems(recentCalls, text)
    }
}

// hide private contacts from recent calls
private fun List<RecentCall>.hidePrivateContacts(privateContacts: List<Contact>, shouldHide: Boolean): List<RecentCall> {
    return if (shouldHide) {
        filterNot { recent ->
            val privateNumbers = privateContacts.flatMap { it.phoneNumbers }.map { it.value }
            recent.phoneNumber in privateNumbers
        }
    } else {
        this
    }
}

private fun List<RecentCall>.setNamesIfEmpty(contacts: List<Contact>, privateContacts: List<Contact>): ArrayList<RecentCall> {
    val contactsWithNumbers = contacts.filter { it.phoneNumbers.isNotEmpty() }
    return map { recent ->
        if (recent.phoneNumber == recent.name) {
            val privateContact = privateContacts.firstOrNull { it.doesContainPhoneNumber(recent.phoneNumber) }
            val contact = contactsWithNumbers.firstOrNull { it.phoneNumbers.first().normalizedNumber == recent.phoneNumber }

            when {
                privateContact != null -> recent.copy(name = privateContact.getNameToDisplay())
                contact != null -> recent.copy(name = contact.getNameToDisplay())
                else -> recent
            }
        } else {
            recent
        }
    } as ArrayList
}

private fun List<RecentCall>.onlyFromKnownContacts(context: Context, contacts: List<Contact>, privateContacts: List<Contact>): List<RecentCall> {
    return filter { recent ->
        val normalizedNumber = recent.phoneNumber.normalizePhoneNumber()
        // Keep only if number is not empty and is saved to device (not SIM)
        normalizedNumber.isNotEmpty() && ContactFiltering.isContactSavedToDevice(context, normalizedNumber)
    }
}

private fun isContactSavedToDevice(context: Context, phoneNumber: String): Boolean {
    if (phoneNumber.isEmpty()) return false

    // Common SIM account types across different manufacturers
    val simAccountTypes = setOf(
        "com.android.contacts.sim",     // Standard Android
        "vnd.sec.contact.sim",          // Samsung
        "com.android.sim",              // Some Android variants
        "sim",                          // Generic
        "SIM"                           // Case variant
    )

    // Normalize the phone number for comparison
    val normalizedNumber = phoneNumber.normalizePhoneNumber()

    // First, get contact IDs that match the phone number
    val lookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
        .appendPath(normalizedNumber)
        .build()

    val contactIds = mutableSetOf<String>()

    context.contentResolver.query(
        lookupUri,
        arrayOf(ContactsContract.PhoneLookup.CONTACT_ID),
        null,
        null,
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val contactId = cursor.getString(0)
            contactIds.add(contactId)
        }
    }

    if (contactIds.isEmpty()) return false

    // Now check if any of these contacts are stored on device (not SIM)
    val contactIdsList = contactIds.joinToString(",")

    // Create dynamic filter for all known SIM account types
    val simAccountTypesPlaceholders = simAccountTypes.joinToString(",") { "?" }
    val selectionArgs = simAccountTypes.toTypedArray()

    context.contentResolver.query(
        ContactsContract.RawContacts.CONTENT_URI,
        arrayOf(ContactsContract.RawContacts._ID),
        "${ContactsContract.RawContacts.CONTACT_ID} IN ($contactIdsList) AND " +
            "(${ContactsContract.RawContacts.ACCOUNT_TYPE} NOT IN ($simAccountTypesPlaceholders) OR ${ContactsContract.RawContacts.ACCOUNT_TYPE} IS NULL)",
        selectionArgs,
        null
    )?.use { cursor ->
        return cursor.count > 0
    }

    return false
}
