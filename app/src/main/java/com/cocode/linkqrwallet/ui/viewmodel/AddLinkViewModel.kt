package com.cocode.linkqrwallet.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocode.linkqrwallet.R
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkRepository
import com.cocode.linkqrwallet.data.TitleFetcher
import com.cocode.linkqrwallet.data.UrlUtils
import com.cocode.linkqrwallet.data.UrlSafety
import com.cocode.linkqrwallet.ui.messageRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddLinkState(
    val rawUrl: String = "",
    val normalizedUrl: String? = null,
    val title: String = "",
    val domain: String = "",
    @StringRes val errorMessage: Int? = null,
    val duplicateId: Long? = null
)

/**
 * Nothing here touches the network until a link is saved: typing, scanning and sharing an
 * address only fill in the screen. After a save, [backgroundScope] reads the page title once,
 * so it finishes even when the screen is already gone.
 */
class AddLinkViewModel(
    private val repository: LinkRepository,
    private val titleFetcher: TitleFetcher,
    private val backgroundScope: CoroutineScope
) : ViewModel() {
    private val state = MutableStateFlow(AddLinkState())
    val uiState: StateFlow<AddLinkState> = state.asStateFlow()

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
    }

    fun updateTitle(value: String) {
        state.value = state.value.copy(title = value)
    }

    fun validateAndSave(onSaved: (Long) -> Unit, onDuplicate: (Long) -> Unit) {
        val normalized = state.value.normalizedUrl
        if (normalized == null) {
            state.value = state.value.copy(errorMessage = R.string.error_enter_url)
            return
        }
        val safety = UrlSafety.check(normalized)
        if (!safety.isSafe) {
            state.value = state.value.copy(errorMessage = safety.reason.messageRes())
            return
        }
        viewModelScope.launch {
            val existing = repository.findByUrl(normalized)
            if (existing != null) {
                state.value = state.value.copy(duplicateId = existing.id)
                onDuplicate(existing.id)
                return@launch
            }
            save(normalized, onSaved)
        }
    }

    fun saveDuplicateAllowed(onSaved: (Long) -> Unit) {
        val normalized = state.value.normalizedUrl ?: return
        if (!UrlSafety.check(normalized).isSafe) return
        viewModelScope.launch { save(normalized, onSaved) }
    }

    /** Saves at once. A page title is read afterwards, and only if the person left the title empty. */
    private suspend fun save(normalized: String, onSaved: (Long) -> Unit) {
        val typedTitle = state.value.title
        val fallbackTitle = UrlUtils.domainFromUrl(normalized)
        val now = System.currentTimeMillis()
        val item = LinkItem(
            url = normalized,
            title = typedTitle.ifBlank { fallbackTitle },
            domain = fallbackTitle,
            createdAt = now,
            updatedAt = now
        )
        val id = repository.insert(item)
        if (typedTitle.isBlank()) readTitleLater(id, normalized, fallbackTitle)
        onSaved(id)
    }

    private fun readTitleLater(id: Long, url: String, fallbackTitle: String) {
        backgroundScope.launch {
            val pageTitle = titleFetcher.fetchTitle(url) ?: return@launch
            repository.replaceTitle(id, expected = fallbackTitle, title = pageTitle)
        }
    }

    fun clearDuplicatePrompt() {
        state.value = state.value.copy(duplicateId = null)
    }
}
