package com.simplemobiletools.dialer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_requests")
data class ContactRequestEntity(
    @PrimaryKey val requestId: String,          // UUID
    val firstName: String,                       // First name
    val lastName: String,                        // Last name (optional)
    val phone: String,                           // Phone number (normalized)
    val requestSource: String,                   // "user" or "sim"
    val requestedAt: Long,                       // Timestamp millis
    val syncedToAgent: Boolean = false,          // Marked true when agent picks up
    val status: String = "pending"               // "pending", "approved", "rejected"
)
