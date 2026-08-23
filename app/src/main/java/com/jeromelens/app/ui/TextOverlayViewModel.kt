package com.jeromelens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeromelens.app.data.ClipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TextOverlayViewModel @Inject constructor(
    private val repository: ClipRepository
) : ViewModel() {

    fun saveClip(text: String, screenshotPath: String?) {
        viewModelScope.launch {
            repository.insert(text, screenshotPath)
        }
    }
}
