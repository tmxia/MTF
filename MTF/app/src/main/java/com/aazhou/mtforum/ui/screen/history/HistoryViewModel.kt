package com.aazhou.mtforum.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.model.HistoryItem
import com.aazhou.mtforum.data.repo.HistoryStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val store: HistoryStore) : ViewModel() {

    val items: StateFlow<List<HistoryItem>> =
        store.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(tid: String, type: String = "thread") = viewModelScope.launch { store.remove(tid, type) }
    fun clear() = viewModelScope.launch { store.clear() }
}
