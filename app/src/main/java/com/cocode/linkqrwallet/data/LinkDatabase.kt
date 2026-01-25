package com.cocode.linkqrwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LinkItem::class], version = 1, exportSchema = false)
abstract class LinkDatabase : RoomDatabase() {
    abstract fun linkItemDao(): LinkItemDao

    companion object {
        @Volatile
        private var INSTANCE: LinkDatabase? = null

        fun getInstance(context: Context): LinkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LinkDatabase::class.java,
                    "linkqrwallet.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
