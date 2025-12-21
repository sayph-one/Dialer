package com.simplemobiletools.dialer.models

import com.simplemobiletools.dialer.database.ContactRequestEntity

sealed class ContactRequestListItem {
    data class Header(
        val title: String,
        val count: Int,
        val isExpanded: Boolean = true
    ) : ContactRequestListItem()

    data class Request(
        val entity: ContactRequestEntity
    ) : ContactRequestListItem()
}
