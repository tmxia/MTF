package com.aazhou.mtforum.ui.screen.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.log.LogEntry
import com.aazhou.mtforum.data.log.LogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch






class LogsViewModel(private val store: LogStore) : ViewModel() {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch { _entries.value = store.entries() }
    }

    fun clear() {
        viewModelScope.launch {
            store.clear()
            _entries.value = emptyList()
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            store.remove(id)
            _entries.value = store.entries()
        }
    }
}


class LogDetailViewModel(
    private val store: LogStore,
    private val id: Long,
) : ViewModel() {

    private val _entry = MutableStateFlow<LogEntry?>(null)
    val entry: StateFlow<LogEntry?> = _entry.asStateFlow()

    init {
        viewModelScope.launch { _entry.value = store.entries().firstOrNull { it.id == id } }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            store.remove(id)
            onDone()
        }
    }
}
