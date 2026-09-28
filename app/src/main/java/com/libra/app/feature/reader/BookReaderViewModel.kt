package com.libra.app.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderState(
    val chapters: List<Chapter> = emptyList(),
    val progress: ReadingProgress? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val progressSyncError: String? = null
)

class BookReaderViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()
    private var chaptersJob: Job? = null
    private var progressJob: Job? = null
    private var currentBookId = ""
    private var currentUserId = ""
    private var localProgressUpdatedAt = 0L

    fun load(book: Book, userId: String, localStore: ReaderPreferencesStore) {
        if (book.id.isBlank() || userId.isBlank()) {
            _state.value = ReaderState(loading = false, error = "Kitap ve kullanıcı bilgisi bulunamadı.")
            return
        }
        if (currentBookId == book.id && currentUserId == userId && _state.value.chapters.isNotEmpty()) return
        currentBookId = book.id
        currentUserId = userId
        chaptersJob?.cancel()
        progressJob?.cancel()

        val local = localStore.readProgress(userId, book.id)
        localProgressUpdatedAt = local?.updatedAt ?: 0L
        _state.value = ReaderState(progress = local, loading = true)

        chaptersJob = viewModelScope.launch {
            ServiceLocator.bookRepository.getBookChapters(book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> _state.value = _state.value.copy(
                        chapters = result.data.filter { it.isPublished }.sortedBy { it.chapterNumber },
                        loading = false,
                        error = null
                    )
                    is AppResult.Error -> _state.value = _state.value.copy(loading = false, error = result.error.message)
                }
            }
        }

        progressJob = viewModelScope.launch {
            ServiceLocator.bookRepository.getReadingProgress(userId, book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val remote = result.data
                        if (remote != null && remote.updatedAt >= localProgressUpdatedAt) {
                            localProgressUpdatedAt = remote.updatedAt
                            localStore.saveProgress(remote)
                            _state.value = _state.value.copy(progress = remote, progressSyncError = null)
                        } else if (remote == null && local != null) {
                            _state.value = _state.value.copy(progress = local)
                        }
                    }
                    is AppResult.Error -> _state.value = _state.value.copy(progressSyncError = result.error.message)
                }
            }
        }
    }

    fun saveProgress(progress: ReadingProgress, localStore: ReaderPreferencesStore) {
        val local = progress.copy(updatedAt = System.currentTimeMillis())
        localProgressUpdatedAt = local.updatedAt
        localStore.saveProgress(local)
        _state.value = _state.value.copy(progress = local, progressSyncError = null)

        viewModelScope.launch {
            when (val result = ServiceLocator.bookRepository.saveReadingProgress(local)) {
                is AppResult.Success -> {
                    val saved = result.data
                    if (saved.updatedAt >= localProgressUpdatedAt) {
                        localProgressUpdatedAt = saved.updatedAt
                        localStore.saveProgress(saved)
                        _state.value = _state.value.copy(progress = saved, progressSyncError = null)
                    }
                }
                is AppResult.Error -> _state.value = _state.value.copy(progressSyncError = result.error.message)
            }
        }
    }

    fun clear() {
        chaptersJob?.cancel()
        progressJob?.cancel()
        currentBookId = ""
        currentUserId = ""
        localProgressUpdatedAt = 0L
        _state.value = ReaderState()
    }
}
