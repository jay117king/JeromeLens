package com.jeromelens.app.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao
) {
    fun getAllClips(): Flow<List<ClipEntity>> = clipDao.getAllClips()

    fun searchClips(query: String): Flow<List<ClipEntity>> = clipDao.searchClips(query)

    suspend fun insert(text: String, screenshotPath: String? = null, sourceApp: String? = null) {
        clipDao.insertClip(
            ClipEntity(
                text = text,
                screenshotPath = screenshotPath,
                sourceApp = sourceApp
            )
        )
    }

    suspend fun toggleFavorite(clip: ClipEntity) {
        clipDao.updateClip(clip.copy(isFavorite = !clip.isFavorite))
    }

    suspend fun delete(clip: ClipEntity) {
        clipDao.deleteClip(clip)
    }
}
