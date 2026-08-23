package com.jeromelens.app.data

import com.jeromelens.app.util.Categories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipRepository @Inject constructor(
    private val clipDao: ClipDao
) {
    fun getAllClips(): Flow<List<ClipEntity>> = clipDao.getAllClips()

    fun searchClips(query: String): Flow<List<ClipEntity>> = clipDao.searchClips(query)

    fun getClipsByCategory(category: String): Flow<List<ClipEntity>> =
        clipDao.getClipsByCategory(category)

    fun searchClipsByCategory(category: String, query: String): Flow<List<ClipEntity>> =
        clipDao.searchClipsByCategory(category, query)

    fun getAllCategories(): Flow<List<String>> = clipDao.getAllCategories()

    suspend fun insert(
        text: String,
        screenshotPath: String? = null,
        sourceApp: String? = null,
        category: String? = null
    ) {
        clipDao.insertClip(
            ClipEntity(
                text = text,
                screenshotPath = screenshotPath,
                sourceApp = sourceApp,
                category = category?.takeIf { it.isNotBlank() } ?: Categories.UNCATEGORIZED
            )
        )
    }

    suspend fun insertBatch(clips: List<ClipEntity>) {
        clipDao.insertClips(clips)
    }

    suspend fun toggleFavorite(clip: ClipEntity) {
        clipDao.updateClip(clip.copy(isFavorite = !clip.isFavorite))
    }

    suspend fun updateCategory(clip: ClipEntity, category: String) {
        clipDao.updateClip(clip.copy(category = category))
    }

    suspend fun delete(clip: ClipEntity) {
        clipDao.deleteClip(clip)
    }
}
