package com.cocode.linkqrwallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cocode.linkqrwallet.data.LinkRepository
import com.cocode.linkqrwallet.data.TitleFetcher

class AppViewModelFactory(
    private val repository: LinkRepository,
    private val titleFetcher: TitleFetcher = TitleFetcher()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(repository) as T
            modelClass.isAssignableFrom(AddLinkViewModel::class.java) ->
                AddLinkViewModel(repository, titleFetcher) as T
            modelClass.isAssignableFrom(DetailViewModel::class.java) ->
                DetailViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}
