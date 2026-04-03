package com.simplemobiletools.dialer.fragments

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.AttributeSet
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.MainActivity
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.adapters.ContactsAdapter
import com.simplemobiletools.dialer.databinding.FragmentContactsBinding
import com.simplemobiletools.dialer.databinding.FragmentLettersLayoutBinding
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.launchCreateNewContactIntent
import com.simplemobiletools.dialer.extensions.startContactDetailsIntent
import com.simplemobiletools.dialer.helpers.ContactFiltering
import com.simplemobiletools.dialer.helpers.DemoDataProvider
import com.simplemobiletools.dialer.interfaces.RefreshItemsListener
import java.util.Locale

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LettersInnerBinding>(context, attributeSet),
    RefreshItemsListener {
    private lateinit var binding: FragmentLettersLayoutBinding
    private var allContacts = ArrayList<Contact>()
    private var contactsLoaded = false
    private var contactsChanged = true
    private var contactsObserver: ContentObserver? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentLettersLayoutBinding.bind(FragmentContactsBinding.bind(this).contactsFragment)
        innerBinding = LettersInnerBinding(binding)
    }

    override fun setupFragment() {
        val placeholderResId = if (context.hasPermission(PERMISSION_READ_CONTACTS)) {
            R.string.no_contacts_found
        } else {
            R.string.could_not_access_contacts
        }

        binding.fragmentPlaceholder.text = context.getString(placeholderResId)

        if (!context.hasPermission(PERMISSION_READ_CONTACTS)) {
            binding.fragmentPlaceholder2.apply {
                text = context.getString(R.string.request_access)
                underlineText()
                setOnClickListener {
                        requestReadContactsPermission()

                }
            }
        }

        setupContactsObserver()
    }

    private fun setupContactsObserver() {
        if (contactsObserver == null) {
            contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    contactsChanged = true
                }
            }

            try {
                context.contentResolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI,
                    true,
                    contactsObserver!!
                )
            } catch (e: Exception) {
                // Ignore if registration fails
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupContactsObserver()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterContactsObserver()
    }

    private fun unregisterContactsObserver() {
        contactsObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // Ignore if unregistration fails
            }
            contactsObserver = null
        }
    }

    override fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int) {
        binding.apply {
            (fragmentList?.adapter as? MyRecyclerViewAdapter)?.updateTextColor(textColor)
            fragmentPlaceholder.setTextColor(textColor)
            fragmentPlaceholder2.setTextColor(properPrimaryColor)

            letterFastscroller.textColor = textColor.getColorStateList()
            letterFastscroller.pressedTextColor = properPrimaryColor
            letterFastscrollerThumb.setupWithFastScroller(letterFastscroller)
            letterFastscrollerThumb.textColor = properPrimaryColor.getContrastColor()
            letterFastscrollerThumb.thumbColor = properPrimaryColor.getColorStateList()
        }
    }

    override fun refreshItems(callback: (() -> Unit)?) {
        // Demo mode: show mock contacts
        if (context.config.demoMode) {
            allContacts = ArrayList(DemoDataProvider.demoContacts)
            contactsLoaded = true
            activity?.runOnUiThread {
                gotContacts(allContacts)
                callback?.invoke()
            }
            return
        }

        // If contacts are already loaded and haven't changed, just display cached data
        if (contactsLoaded && !contactsChanged) {
            gotContacts(allContacts)
            callback?.invoke()
            return
        }

        // Load contacts from system
        ContactsHelper(context).getContacts(showOnlyContactsWithNumbers = true) { contacts ->
            // Filter out SIM contacts
            allContacts = ContactFiltering.filterDeviceContacts(context, contacts)

            // Try to load private contacts from Simple Commons provider (may not exist)
            try {
                if (SMT_PRIVATE !in context.baseConfig.ignoredContactSources) {
                    val privateCursor = context?.getMyContactsCursor(false, true)
                    val privateContacts = MyContactsContentProvider.getContacts(context, privateCursor)
                    if (privateContacts.isNotEmpty()) {
                        // Also filter private contacts
                        val filteredPrivateContacts = ContactFiltering.filterDeviceContacts(context, privateContacts)
                        allContacts.addAll(filteredPrivateContacts)
                        allContacts.sort()
                    }
                }
            } catch (e: Exception) {
                // Private contacts provider not available - this is expected
            }

            (activity as MainActivity).cacheContacts(allContacts)

            // Mark as loaded and reset changed flag
            contactsLoaded = true
            contactsChanged = false

            activity?.runOnUiThread {
                gotContacts(allContacts)
                callback?.invoke()
            }
        }
    }

    private fun gotContacts(contacts: ArrayList<Contact>) {
        setupLetterFastScroller(contacts)
        if (contacts.isEmpty()) {
            binding.apply {
                fragmentPlaceholder.beVisible()
                fragmentPlaceholder2.beVisible()
                fragmentList.beGone()
            }
        } else {
            binding.apply {
                fragmentPlaceholder.beGone()
                fragmentPlaceholder2.beGone()
                fragmentList.beVisible()
            }

            if (binding.fragmentList.adapter == null) {
                ContactsAdapter(
                    activity = activity as SimpleActivity,
                    contacts = contacts,
                    recyclerView = binding.fragmentList,
                    refreshItemsListener = this
                ) {
                    val contact = it as Contact
                    activity?.startContactDetailsIntent(contact)
                }.apply {
                    binding.fragmentList.adapter = this
                }

                if (context.areSystemAnimationsEnabled) {
                    binding.fragmentList.scheduleLayoutAnimation()
                }
            } else {
                (binding.fragmentList.adapter as ContactsAdapter).updateItems(contacts)
            }
        }
    }

    private fun setupLetterFastScroller(contacts: ArrayList<Contact>) {
        binding.letterFastscroller.setupWithRecyclerView(binding.fragmentList, { position ->
            try {
                val name = contacts[position].getNameToDisplay()
                val character = if (name.isNotEmpty()) name.substring(0, 1) else ""
                FastScrollItemIndicator.Text(character.uppercase(Locale.getDefault()).normalizeString())
            } catch (e: Exception) {
                FastScrollItemIndicator.Text("")
            }
        })
    }

    override fun onSearchClosed() {
        binding.fragmentPlaceholder.beVisibleIf(allContacts.isEmpty())
        (binding.fragmentList.adapter as? ContactsAdapter)?.updateItems(allContacts)
        setupLetterFastScroller(allContacts)
    }

    override fun onSearchQueryChanged(text: String) {
        val shouldNormalize = text.normalizeString() == text
        val filtered = allContacts.filter {
            getProperText(it.getNameToDisplay(), shouldNormalize).contains(text, true) ||
                getProperText(it.nickname, shouldNormalize).contains(text, true) ||
                it.phoneNumbers.any {
                    text.normalizePhoneNumber().isNotEmpty() && it.normalizedNumber.contains(text.normalizePhoneNumber(), true)
                } ||
                it.emails.any { it.value.contains(text, true) } ||
                it.addresses.any { getProperText(it.value, shouldNormalize).contains(text, true) } ||
                it.IMs.any { it.value.contains(text, true) } ||
                getProperText(it.notes, shouldNormalize).contains(text, true) ||
                getProperText(it.organization.company, shouldNormalize).contains(text, true) ||
                getProperText(it.organization.jobPosition, shouldNormalize).contains(text, true) ||
                it.websites.any { it.contains(text, true) }
        } as ArrayList

        filtered.sortBy {
            val nameToDisplay = it.getNameToDisplay()
            !getProperText(nameToDisplay, shouldNormalize).startsWith(text, true) && !nameToDisplay.contains(text, true)
        }

        binding.fragmentPlaceholder.beVisibleIf(filtered.isEmpty())
        (binding.fragmentList.adapter as? ContactsAdapter)?.updateItems(filtered, text)
        setupLetterFastScroller(filtered)
    }

    private fun requestReadContactsPermission() {
        activity?.handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                binding.fragmentPlaceholder.text = context.getString(R.string.no_contacts_found)
                // Force reload contacts after permission is granted
                contactsChanged = true
                refreshItems(null)
            }
        }
    }
}
