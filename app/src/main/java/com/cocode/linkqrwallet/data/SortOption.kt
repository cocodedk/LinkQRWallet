package com.cocode.linkqrwallet.data

import androidx.annotation.StringRes
import com.cocode.linkqrwallet.R

enum class SortOption(@StringRes val label: Int, val orderByClause: String) {
    Newest(R.string.sort_newest, "createdAt DESC"),
    Oldest(R.string.sort_oldest, "createdAt ASC"),
    TitleAsc(R.string.sort_title_asc, "title COLLATE NOCASE ASC"),
    TitleDesc(R.string.sort_title_desc, "title COLLATE NOCASE DESC"),
    DomainAsc(R.string.sort_website_asc, "domain COLLATE NOCASE ASC")
}
