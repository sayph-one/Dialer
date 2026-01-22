package com.simplemobiletools.dialer.helpers

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.commons.models.contacts.Contact

object ContactFiltering {

    // Common SIM account types across different manufacturers
    private val SIM_ACCOUNT_TYPES = setOf(
        "com.android.contacts.sim",     // Standard Android
        "vnd.sec.contact.sim",          // Samsung
        "com.android.sim",              // Some Android variants
        "sim",                          // Generic
        "SIM"                           // Case variant
    )

    private const val TAG = "ContactFiltering"

    /**
     * Check if a phone number belongs to a contact saved to device (not SIM)
     */
    fun isContactSavedToDevice(context: Context, phoneNumber: String, enableDebugLogging: Boolean = false): Boolean {
        if (enableDebugLogging) {
            Log.d(TAG, "=== Checking contact for number: $phoneNumber ===")
        }

        if (phoneNumber.isEmpty()) {
            if (enableDebugLogging) Log.d(TAG, "Phone number is empty, returning false")
            return false
        }

        // Normalize the phone number for comparison
        val normalizedNumber = phoneNumber.normalizePhoneNumber()
        if (enableDebugLogging) Log.d(TAG, "Normalized number: $normalizedNumber")

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
            if (enableDebugLogging) Log.d(TAG, "PhoneLookup query returned ${cursor.count} results")
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(0)
                contactIds.add(contactId)
                if (enableDebugLogging) Log.d(TAG, "Found contact ID: $contactId")
            }
        }

        if (contactIds.isEmpty()) {
            if (enableDebugLogging) Log.d(TAG, "No contact IDs found, returning false")
            return false
        }

        // Now check if any of these contacts are stored on device (not SIM)
        val contactIdsList = contactIds.joinToString(",")

        if (enableDebugLogging) {
            // Debug: show all raw contacts for these IDs
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts._ID,
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    ContactsContract.RawContacts.ACCOUNT_NAME
                ),
                "${ContactsContract.RawContacts.CONTACT_ID} IN ($contactIdsList)",
                null,
                null
            )?.use { cursor ->
                Log.d(TAG, "Raw contacts query returned ${cursor.count} results")
                while (cursor.moveToNext()) {
                    val rawContactId = cursor.getString(0)
                    val contactId = cursor.getString(1)
                    val accountType = cursor.getString(2)
                    val accountName = cursor.getString(3)

                    Log.d(TAG, "Raw Contact - ID: $rawContactId, Contact ID: $contactId, Account Type: '$accountType', Account Name: '$accountName'")

                    if (accountType in SIM_ACCOUNT_TYPES) {
                        Log.w(TAG, "*** SIM CONTACT DETECTED *** - Account Type: '$accountType'")
                    }
                }
            }
        }

        // Create dynamic filter for all known SIM account types
        val simAccountTypesPlaceholders = SIM_ACCOUNT_TYPES.joinToString(",") { "?" }
        val selectionArgs = SIM_ACCOUNT_TYPES.toTypedArray()

        // Accept contacts with:
        // 1. NULL account type (local device contacts created by apps like SayphAgent)
        // 2. Non-SIM account types
        // SIM contacts on Samsung have account_type like "vnd.sec.contact.sim"
        return context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} IN ($contactIdsList) AND " +
                "(${ContactsContract.RawContacts.ACCOUNT_TYPE} IS NULL OR " +
                "${ContactsContract.RawContacts.ACCOUNT_TYPE} NOT IN ($simAccountTypesPlaceholders))",
            selectionArgs,
            null
        )?.use { cursor ->
            val count = cursor.count
            if (enableDebugLogging) {
                Log.d(TAG, "Filtered query (excluding SIM types, allowing NULL/local) returned $count results")
            }
            count > 0
        } ?: false
    }

    /**
     * Filter a list of contacts to only include those saved to device (not SIM).
     * Uses the contact's own source field to determine if it's a SIM contact,
     * rather than phone number lookup (which can be fooled by duplicate numbers).
     */
    fun filterDeviceContacts(context: Context, contacts: List<Contact>, enableDebugLogging: Boolean = false): ArrayList<Contact> {
        if (enableDebugLogging) {
            Log.d(TAG, "Filtering ${contacts.size} contacts to exclude SIM contacts")
        }

        val filtered = contacts.filter { contact ->
            // Check if this contact's source indicates it's a SIM contact
            val isSimContact = isSimSource(contact.source)

            if (enableDebugLogging) {
                Log.d(TAG, "Contact: ${contact.getNameToDisplay()}, source: '${contact.source}', isSimContact: $isSimContact")
            }

            if (isSimContact) {
                if (enableDebugLogging) {
                    Log.d(TAG, "Filtering out SIM contact: ${contact.getNameToDisplay()} (source: ${contact.source})")
                }
            }

            !isSimContact
        } as ArrayList<Contact>

        if (enableDebugLogging) {
            Log.d(TAG, "Filtered result: ${filtered.size} device contacts (${contacts.size - filtered.size} SIM contacts removed)")
        }

        return filtered
    }

    /**
     * Check if a contact source indicates a SIM contact.
     * The source field contains the account name (e.g., "primary.sim.account_name" for Samsung SIM).
     */
    private fun isSimSource(source: String?): Boolean {
        if (source.isNullOrEmpty()) return false // Empty source is usually device contact

        // Check if source contains "sim" (case insensitive)
        // This catches: "primary.sim.account_name", "SIM", "sim", etc.
        return source.contains("sim", ignoreCase = true)
    }

    /**
     * Extension function for filtering recent calls to only include device contacts
     */
    fun <T> List<T>.onlyFromDeviceContacts(
        context: Context,
        phoneNumberExtractor: (T) -> String,
        enableDebugLogging: Boolean = false
    ): List<T> {
        return filter { item ->
            val phoneNumber = phoneNumberExtractor(item).normalizePhoneNumber()
            phoneNumber.isNotEmpty() && isContactSavedToDevice(context, phoneNumber, enableDebugLogging)
        }
    }
}
