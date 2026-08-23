package com.jeromelens.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClipEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
}
