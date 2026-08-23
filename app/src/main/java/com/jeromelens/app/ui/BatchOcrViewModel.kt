package com.jeromelens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeromelens.app.data.ClipEntity
import com.jeromelens.app.data.ClipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchOcrViewModel @Inject constructor(
    private val repository: ClipRepository
) : ViewModel() {

    fun saveBatch(
        results: List<Pair<String, String?>>,
        category: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val clips = results.map { (text, path) ->
                ClipEntity(
                    text = text,
                    screenshotPath = path,
                    category = category,
                    sourceApp = "gallery"
                )
            }
            repository.insertBatch(clips)
            onDone()
        }
    }
}
