package com.cocode.linkqrwallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: LinkRepository
) : ViewModel() {
    fun observeLink(id: Long): Flow<LinkItem?> = repository.observeById(id)

    fun update(item: LinkItem) {
        viewModelScope.launch {
            repository.update(item.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(item: LinkItem, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(item)
            onDeleted()
        }
    }
}
