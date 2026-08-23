package com.jeromelens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeromelens.app.data.ClipEntity
import com.jeromelens.app.data.ClipRepository
import com.jeromelens.app.util.Categories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ClipRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null) // null = All

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clips = combine(searchQuery, selectedCategory) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        when {
            category != null && query.isNotBlank() ->
                repository.searchClipsByCategory(category, query)
            category != null ->
                repository.getClipsByCategory(category)
            query.isNotBlank() ->
                repository.searchClips(query)
            else ->
                repository.getAllClips()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) {
        searchQuery.value = query
    }

    fun filterByCategory(category: String?) {
        selectedCategory.value = category
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

    fun updateCategory(clip: ClipEntity, category: String) {
        viewModelScope.launch {
            repository.updateCategory(clip, category.ifBlank { Categories.UNCATEGORIZED })
        }
    }
}
