package com.simplemobiletools.dialer.helpers

import android.provider.CallLog
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.dialer.database.ContactRequestEntity
import com.simplemobiletools.dialer.models.RecentCall

/**
 * Provides demo data for video demonstration purposes.
 * Uses UK Ofcom TV-safe phone numbers (07700 900000-900999).
 */
object DemoDataProvider {

    // Session-based storage for requests added during demo mode (in-memory only)
    private val sessionRequests = mutableListOf<ContactRequestEntity>()

    /**
     * Add a contact request during demo session (stored in memory only)
     */
    fun addSessionRequest(request: ContactRequestEntity) {
        sessionRequests.add(0, request)  // Add at beginning (newest first)
    }

    /**
     * Clear session requests (called when toggling demo mode)
     */
    fun clearSessionRequests() {
        sessionRequests.clear()
    }

    /**
     * Get all contact requests for demo mode (session + demo data)
     */
    fun getAllDemoRequests(status: String): List<ContactRequestEntity> {
        val sessionFiltered = sessionRequests.filter { it.status == status }
        val demoFiltered = demoContactRequests.filter { it.status == status }
        return sessionFiltered + demoFiltered
    }

    private data class DemoContactData(
        val name: String,
        val phone: String,
        val starred: Boolean = false
    )

    private val demoContactsData = listOf(
        DemoContactData("Mum", "07700 900001", starred = true),
        DemoContactData("Dad", "07700 900002", starred = true),
        DemoContactData("Grandma", "07700 900003"),
        DemoContactData("Grandpa", "07700 900004"),
        DemoContactData("Granny", "07700 900005"),
        DemoContactData("Granddad", "07700 900006"),
        DemoContactData("Milo", "07700 900007"),
        DemoContactData("Daisy", "07700 900008"),
        DemoContactData("Henry", "07700 900009"),
        DemoContactData("Isla", "07700 900010")
    )

    /**
     * Demo contacts list - sorted alphabetically by name
     */
    val demoContacts: List<Contact> by lazy {
        demoContactsData.mapIndexed { index, data ->
            createContact(
                id = index + 1,
                name = data.name,
                phone = data.phone,
                starred = data.starred
            )
        }.sortedBy { it.getNameToDisplay() }
    }

    /**
     * Demo favorites - only starred contacts
     */
    val demoFavorites: List<Contact> by lazy {
        demoContacts.filter { it.starred == 1 }
    }

    /**
     * Demo call history with varied call types and timestamps
     */
    val demoRecentCalls: List<RecentCall> by lazy {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour

        listOf(
            // Today
            createRecentCall(
                id = 1,
                name = "Mum",
                phone = "07700900001",
                type = CallLog.Calls.MISSED_TYPE,
                timestamp = now - hour,  // 1 hour ago
                duration = 0
            ),
            createRecentCall(
                id = 2,
                name = "Dad",
                phone = "07700900002",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (2 * hour),  // 2 hours ago
                duration = 180  // 3 minutes
            ),
            createRecentCall(
                id = 3,
                name = "Milo",
                phone = "07700900007",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (4 * hour),  // 4 hours ago
                duration = 45  // 45 seconds
            ),
            // Yesterday
            createRecentCall(
                id = 4,
                name = "Grandma",
                phone = "07700900003",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - day,  // yesterday
                duration = 420  // 7 minutes
            ),
            createRecentCall(
                id = 5,
                name = "Daisy",
                phone = "07700900008",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - day - (2 * hour),
                duration = 125  // ~2 minutes
            ),
            createRecentCall(
                id = 6,
                name = "Mum",
                phone = "07700900001",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - day - (5 * hour),
                duration = 540  // 9 minutes
            ),
            // 2 days ago
            createRecentCall(
                id = 7,
                name = "Isla",
                phone = "07700900010",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (2 * day),
                duration = 720  // 12 minutes
            ),
            createRecentCall(
                id = 8,
                name = "Henry",
                phone = "07700900009",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (2 * day) - (3 * hour),
                duration = 60  // 1 minute
            ),
            createRecentCall(
                id = 9,
                name = "Dad",
                phone = "07700900002",
                type = CallLog.Calls.MISSED_TYPE,
                timestamp = now - (2 * day) - (6 * hour),
                duration = 0
            ),
            // 3 days ago
            createRecentCall(
                id = 10,
                name = "Granny",
                phone = "07700900005",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (3 * day),
                duration = 480  // 8 minutes
            ),
            createRecentCall(
                id = 11,
                name = "Milo",
                phone = "07700900007",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (3 * day) - (4 * hour),
                duration = 90  // 1.5 minutes
            ),
            // 4 days ago
            createRecentCall(
                id = 12,
                name = "Grandpa",
                phone = "07700900004",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (4 * day),
                duration = 300  // 5 minutes
            ),
            createRecentCall(
                id = 13,
                name = "Mum",
                phone = "07700900001",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (4 * day) - (2 * hour),
                duration = 195  // 3+ minutes
            ),
            // 5 days ago
            createRecentCall(
                id = 14,
                name = "Daisy",
                phone = "07700900008",
                type = CallLog.Calls.MISSED_TYPE,
                timestamp = now - (5 * day),
                duration = 0
            ),
            createRecentCall(
                id = 15,
                name = "Granddad",
                phone = "07700900006",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (5 * day) - (3 * hour),
                duration = 660  // 11 minutes
            ),
            // 6 days ago
            createRecentCall(
                id = 16,
                name = "Isla",
                phone = "07700900010",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (6 * day),
                duration = 240  // 4 minutes
            ),
            createRecentCall(
                id = 17,
                name = "Dad",
                phone = "07700900002",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (6 * day) - (5 * hour),
                duration = 360  // 6 minutes
            ),
            // 1 week ago
            createRecentCall(
                id = 18,
                name = "Henry",
                phone = "07700900009",
                type = CallLog.Calls.MISSED_TYPE,
                timestamp = now - (7 * day),
                duration = 0
            ),
            createRecentCall(
                id = 19,
                name = "Grandma",
                phone = "07700900003",
                type = CallLog.Calls.OUTGOING_TYPE,
                timestamp = now - (7 * day) - (2 * hour),
                duration = 510  // 8.5 minutes
            ),
            createRecentCall(
                id = 20,
                name = "Mum",
                phone = "07700900001",
                type = CallLog.Calls.INCOMING_TYPE,
                timestamp = now - (7 * day) - (6 * hour),
                duration = 780  // 13 minutes
            )
        )
    }

    /**
     * Demo contact requests with pre-populated data across all segments
     */
    val demoContactRequests: List<ContactRequestEntity> by lazy {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour

        listOf(
            // Pending requests
            ContactRequestEntity(
                requestId = "demo-pending-1",
                firstName = "Emma",
                lastName = "Wilson",
                phone = "07700900101",
                requestSource = "user",
                requestedAt = now - (2 * hour),  // 2 hours ago
                syncedToAgent = false,
                status = "pending"
            ),
            ContactRequestEntity(
                requestId = "demo-pending-2",
                firstName = "Jack",
                lastName = "Brown",
                phone = "07700900102",
                requestSource = "user",
                requestedAt = now - day,  // yesterday
                syncedToAgent = false,
                status = "pending"
            ),
            // Approved requests
            ContactRequestEntity(
                requestId = "demo-approved-1",
                firstName = "Oliver",
                lastName = "Smith",
                phone = "07700900100",
                requestSource = "user",
                requestedAt = now - (2 * day),
                syncedToAgent = true,
                status = "approved"
            ),
            ContactRequestEntity(
                requestId = "demo-approved-2",
                firstName = "Sophie",
                lastName = "Taylor",
                phone = "07700900103",
                requestSource = "user",
                requestedAt = now - (4 * day),
                syncedToAgent = true,
                status = "approved"
            ),
            // Rejected requests
            ContactRequestEntity(
                requestId = "demo-rejected-1",
                firstName = "Ryan",
                lastName = "Mitchell",
                phone = "07700900998",
                requestSource = "user",
                requestedAt = now - (3 * day),
                syncedToAgent = true,
                status = "rejected"
            ),
            ContactRequestEntity(
                requestId = "demo-rejected-2",
                firstName = "Chloe",
                lastName = "Parker",
                phone = "07700900999",
                requestSource = "user",
                requestedAt = now - (5 * day),
                syncedToAgent = true,
                status = "rejected"
            )
        )
    }

    private fun createContact(id: Int, name: String, phone: String, starred: Boolean): Contact {
        val normalizedPhone = phone.replace(" ", "")
        val phoneNumber = PhoneNumber(
            value = phone,
            type = 1,  // TYPE_HOME
            label = "",
            normalizedNumber = normalizedPhone,
            isPrimary = true
        )

        return Contact(
            id = id,
            prefix = "",
            firstName = name,
            middleName = "",
            surname = "",
            suffix = "",
            nickname = "",
            photoUri = "",
            phoneNumbers = arrayListOf(phoneNumber),
            emails = arrayListOf(),
            addresses = arrayListOf(),
            events = arrayListOf(),
            source = "Demo",
            starred = if (starred) 1 else 0,
            contactId = id,
            thumbnailUri = "",
            photo = null,
            notes = "",
            groups = arrayListOf(),
            organization = com.simplemobiletools.commons.models.contacts.Organization("", ""),
            websites = arrayListOf(),
            IMs = arrayListOf(),
            mimetype = "",
            ringtone = null
        )
    }

    private fun createRecentCall(
        id: Int,
        name: String,
        phone: String,
        type: Int,
        timestamp: Long,
        duration: Int
    ): RecentCall {
        return RecentCall(
            id = id,
            phoneNumber = phone,
            name = name,
            photoUri = "",
            startTS = (timestamp / 1000).toInt(),
            duration = duration,
            type = type,
            neighbourIDs = mutableListOf(),
            simID = 1,
            specificNumber = phone,
            specificType = "",
            isUnknownNumber = false
        )
    }
}
