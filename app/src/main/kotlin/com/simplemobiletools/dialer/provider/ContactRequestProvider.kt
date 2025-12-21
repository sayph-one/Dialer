package com.simplemobiletools.dialer.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.simplemobiletools.dialer.database.ContactRequestDatabase
import com.simplemobiletools.dialer.database.ContactRequestEntity
import kotlinx.coroutines.runBlocking

class ContactRequestProvider : ContentProvider() {
    companion object {
        private const val TAG = "ContactRequestProvider"
        const val AUTHORITY = "com.simplemobiletools.dialer.contactrequests"
        const val AUTHORITY_DEBUG = "com.simplemobiletools.dialer.debug.contactrequests"

        private const val UNSYNCED = 1
        private const val MARK_SYNCED = 2
        private const val UPDATE_STATUS = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "unsynced", UNSYNCED)
            addURI(AUTHORITY, "mark_synced/*", MARK_SYNCED)
            addURI(AUTHORITY, "update_status/*/*", UPDATE_STATUS)

            addURI(AUTHORITY_DEBUG, "unsynced", UNSYNCED)
            addURI(AUTHORITY_DEBUG, "mark_synced/*", MARK_SYNCED)
            addURI(AUTHORITY_DEBUG, "update_status/*/*", UPDATE_STATUS)
        }

        private val COLUMNS = arrayOf(
            "request_id",
            "first_name",
            "last_name",
            "phone",
            "request_source",
            "requested_at"
        )
    }

    private lateinit var database: ContactRequestDatabase

    override fun onCreate(): Boolean {
        database = ContactRequestDatabase.getDatabase(context!!)
        Log.d(TAG, "ContactRequestProvider initialized")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            UNSYNCED -> {
                Log.d(TAG, "Query: unsynced requests")
                val requests = runBlocking { database.contactRequestDao().getUnsyncedRequests() }
                Log.d(TAG, "Found ${requests.size} unsynced requests")
                createCursor(requests)
            }
            else -> {
                Log.w(TAG, "Unknown query URI: $uri")
                null
            }
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return when (uriMatcher.match(uri)) {
            MARK_SYNCED -> {
                val idsString = uri.lastPathSegment
                if (idsString != null) {
                    val ids = idsString.split(",")
                    Log.d(TAG, "Marking ${ids.size} requests as synced")
                    runBlocking { database.contactRequestDao().markAsSynced(ids) }
                    context?.contentResolver?.notifyChange(uri, null)
                    ids.size
                } else {
                    0
                }
            }
            UPDATE_STATUS -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 3) {
                    val requestId = pathSegments[1]
                    val status = pathSegments[2]
                    Log.d(TAG, "Updating request $requestId to status: $status")
                    runBlocking { database.contactRequestDao().updateStatus(requestId, status) }
                    context?.contentResolver?.notifyChange(uri, null)
                    1
                } else {
                    0
                }
            }
            else -> {
                Log.w(TAG, "Unknown update URI: $uri")
                0
            }
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Not needed for this implementation
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // Not needed for this implementation
        return 0
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            UNSYNCED -> "vnd.android.cursor.dir/vnd.$AUTHORITY.unsynced"
            else -> null
        }
    }

    private fun createCursor(requests: List<ContactRequestEntity>): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        requests.forEach { request ->
            cursor.addRow(arrayOf(
                request.requestId,
                request.firstName,
                request.lastName,
                request.phone,
                request.requestSource,
                request.requestedAt.toString()
            ))
        }
        return cursor
    }
}
