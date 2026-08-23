package com.jeromelens.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchClips(query: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Update
    suspend fun updateClip(clip: ClipEntity)

    @Delete
    suspend fun deleteClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
