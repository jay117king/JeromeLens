package com.jeromelens.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val screenshotPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String? = null,
    val isFavorite: Boolean = false,
    val category: String? = null
)
