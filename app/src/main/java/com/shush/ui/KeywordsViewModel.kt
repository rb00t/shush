package com.shush.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shush.data.KeywordsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeywordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = KeywordsRepository(app)

    val keywords = repository.keywords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Initial value null = "not yet loaded" (blank screen). Upstream emits only
    // true/false, so the gate opens as soon as DataStore's first value arrives.
    val isOnboarded: kotlinx.coroutines.flow.StateFlow<Boolean?> = repository.isOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val blockingEnabled = repository.blockingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setBlockingEnabled(enabled)
    }

    fun add(keyword: String) = viewModelScope.launch { repository.add(keyword) }
    fun remove(keyword: String) = viewModelScope.launch { repository.remove(keyword) }
    fun completeOnboarding() = viewModelScope.launch { repository.completeOnboarding() }
}
