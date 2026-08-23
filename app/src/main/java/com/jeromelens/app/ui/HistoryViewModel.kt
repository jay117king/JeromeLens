package com.jeromelens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeromelens.app.data.ClipEntity
import com.jeromelens.app.data.ClipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ClipRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val clips = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllClips()
        } else {
            repository.searchClips(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(clip: ClipEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(clip)
        }
    }

    fun delete(clip: ClipEntity) {
        viewModelScope.launch {
            repository.delete(clip)
        }
    }
}
