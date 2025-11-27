package com.simplemobiletools.dialer.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactRequestDao {
    @Query("SELECT * FROM contact_requests WHERE status = 'pending'")
    suspend fun getUnsyncedRequests(): List<ContactRequestEntity>

    @Query("SELECT * FROM contact_requests WHERE status = 'pending' ORDER BY requestedAt DESC")
    fun getPendingRequests(): Flow<List<ContactRequestEntity>>

    @Query("SELECT * FROM contact_requests WHERE status = 'approved' ORDER BY requestedAt DESC")
    fun getApprovedRequests(): Flow<List<ContactRequestEntity>>

    @Query("SELECT * FROM contact_requests WHERE status = 'rejected' ORDER BY requestedAt DESC")
    fun getRejectedRequests(): Flow<List<ContactRequestEntity>>

    @Query("SELECT * FROM contact_requests ORDER BY requestedAt DESC")
    fun getAllRequests(): Flow<List<ContactRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ContactRequestEntity): Long

    @Query("UPDATE contact_requests SET syncedToAgent = 1 WHERE requestId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>): Int

    @Query("UPDATE contact_requests SET status = :status WHERE requestId = :id")
    suspend fun updateStatus(id: String, status: String): Int

    @Query("DELETE FROM contact_requests WHERE requestId = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM contact_requests WHERE status = 'rejected'")
    suspend fun deleteAllRejected(): Int
}
