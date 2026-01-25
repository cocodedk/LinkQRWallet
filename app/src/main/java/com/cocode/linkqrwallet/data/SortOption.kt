package com.cocode.linkqrwallet.data

enum class SortOption(val label: String, val orderByClause: String) {
    Newest("Newest", "createdAt DESC"),
    Oldest("Oldest", "createdAt ASC"),
    TitleAsc("Title A-Z", "title COLLATE NOCASE ASC"),
    TitleDesc("Title Z-A", "title COLLATE NOCASE DESC"),
    DomainAsc("Domain A-Z", "domain COLLATE NOCASE ASC")
}
