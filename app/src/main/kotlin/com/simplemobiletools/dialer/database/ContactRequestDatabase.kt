package com.simplemobiletools.dialer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ContactRequestEntity::class], version = 2, exportSchema = false)
abstract class ContactRequestDatabase : RoomDatabase() {
    abstract fun contactRequestDao(): ContactRequestDao

    companion object {
        @Volatile
        private var INSTANCE: ContactRequestDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add firstName and lastName columns
                database.execSQL("ALTER TABLE contact_requests ADD COLUMN firstName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contact_requests ADD COLUMN lastName TEXT NOT NULL DEFAULT ''")

                // Copy name to firstName
                database.execSQL("UPDATE contact_requests SET firstName = name")

                // Drop the old name column
                database.execSQL("CREATE TABLE contact_requests_new (requestId TEXT NOT NULL PRIMARY KEY, firstName TEXT NOT NULL, lastName TEXT NOT NULL, phone TEXT NOT NULL, requestSource TEXT NOT NULL, requestedAt INTEGER NOT NULL, syncedToAgent INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'pending')")
                database.execSQL("INSERT INTO contact_requests_new SELECT requestId, firstName, lastName, phone, requestSource, requestedAt, syncedToAgent, status FROM contact_requests")
                database.execSQL("DROP TABLE contact_requests")
                database.execSQL("ALTER TABLE contact_requests_new RENAME TO contact_requests")
            }
        }

        fun getDatabase(context: Context): ContactRequestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactRequestDatabase::class.java,
                    "contact_requests.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
