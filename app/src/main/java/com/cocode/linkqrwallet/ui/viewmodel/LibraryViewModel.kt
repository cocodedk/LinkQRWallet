package com.cocode.linkqrwallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkRepository
import com.cocode.linkqrwallet.data.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    private val repository: LinkRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sortOption = MutableStateFlow(SortOption.Newest)

    val searchQuery: StateFlow<String> = query.asStateFlow()
    val currentSort: StateFlow<SortOption> = sortOption.asStateFlow()

    val links: StateFlow<List<LinkItem>> = combine(query, sortOption) { term, sort ->
        term to sort
    }.flatMapLatest { (term, sort) ->
        repository.observeLinks(term, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun updateSort(option: SortOption) {
        sortOption.value = option
    }
}
