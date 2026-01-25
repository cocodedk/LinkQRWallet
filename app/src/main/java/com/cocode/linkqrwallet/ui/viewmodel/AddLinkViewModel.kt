package com.cocode.linkqrwallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkRepository
import com.cocode.linkqrwallet.data.TitleFetcher
import com.cocode.linkqrwallet.data.UrlUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddLinkState(
    val rawUrl: String = "",
    val normalizedUrl: String? = null,
    val title: String = "",
    val domain: String = "",
    val isFetchingTitle: Boolean = false,
    val errorMessage: String? = null,
    val duplicateId: Long? = null
)

class AddLinkViewModel(
    private val repository: LinkRepository,
    private val titleFetcher: TitleFetcher
) : ViewModel() {
    private val state = MutableStateFlow(AddLinkState())
    val uiState: StateFlow<AddLinkState> = state.asStateFlow()

    private var fetchJob: Job? = null

    fun updateUrl(value: String) {
        val normalized = UrlUtils.normalizeUrl(value)
        val domain = normalized?.let { UrlUtils.domainFromUrl(it) } ?: ""
        state.value = state.value.copy(
            rawUrl = value,
            normalizedUrl = normalized,
            domain = domain,
            errorMessage = null,
            duplicateId = null
        )
        scheduleTitleFetch(normalized)
    }

    fun updateTitle(value: String) {
        state.value = state.value.copy(title = value)
    }

    private fun scheduleTitleFetch(normalized: String?) {
        fetchJob?.cancel()
        if (normalized == null) return
        fetchJob = viewModelScope.launch {
            delay(400)
            fetchTitle(normalized)
        }
    }

    fun fetchTitle(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            state.value = state.value.copy(isFetchingTitle = true)
            val title = titleFetcher.fetchTitle(url)
            val currentTitle = state.value.title
            val fallbackTitle = UrlUtils.domainFromUrl(url)
            val nextTitle = when {
                title != null && currentTitle.isBlank() -> title
                title != null && currentTitle == fallbackTitle -> title
                currentTitle.isBlank() -> fallbackTitle
                else -> currentTitle
            }
            state.value = state.value.copy(
                title = nextTitle,
                isFetchingTitle = false
            )
        }
    }

    fun validateAndSave(onSaved: (Long) -> Unit, onDuplicate: (Long) -> Unit) {
        val normalized = state.value.normalizedUrl
        if (normalized == null) {
            state.value = state.value.copy(errorMessage = "Enter a valid URL.")
            return
        }
        val currentTitle = state.value.title.ifBlank { UrlUtils.domainFromUrl(normalized) }
        viewModelScope.launch {
            val existing = repository.findByUrl(normalized)
            if (existing != null) {
                state.value = state.value.copy(duplicateId = existing.id)
                onDuplicate(existing.id)
                return@launch
            }
            val now = System.currentTimeMillis()
            val item = LinkItem(
                url = normalized,
                title = currentTitle,
                domain = UrlUtils.domainFromUrl(normalized),
                createdAt = now,
                updatedAt = now
            )
            val id = repository.insert(item)
            onSaved(id)
        }
    }

    fun saveDuplicateAllowed(onSaved: (Long) -> Unit) {
        val normalized = state.value.normalizedUrl ?: return
        val currentTitle = state.value.title.ifBlank { UrlUtils.domainFromUrl(normalized) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val item = LinkItem(
                url = normalized,
                title = currentTitle,
                domain = UrlUtils.domainFromUrl(normalized),
                createdAt = now,
                updatedAt = now
            )
            val id = repository.insert(item)
            onSaved(id)
        }
    }

    fun clearDuplicatePrompt() {
        state.value = state.value.copy(duplicateId = null)
    }
}
