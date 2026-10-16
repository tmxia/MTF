package com.aazhou.mtforum.ui.screen.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.mtforum.data.download.ThreadArchiveStore
import com.aazhou.mtforum.data.model.DownloadedThread
import com.aazhou.mtforum.data.repo.DownloadStore
import com.aazhou.mtforum.ui.common.deleteThreadDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch





class DownloadsViewModel(
    private val downloadStore: DownloadStore,
    private val archiveStore: ThreadArchiveStore,
) : ViewModel() {

    val items: Flow<List<DownloadedThread>> = downloadStore.items

    fun delete(tid: String) {
        viewModelScope.launch {
            deleteThreadDownload(tid, downloadStore, archiveStore)
        }
    }
}
