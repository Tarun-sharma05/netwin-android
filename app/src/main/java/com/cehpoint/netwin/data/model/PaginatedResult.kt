package com.cehpoint.netwin.data.model

data class PaginatedResult<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val lastDocument: String? = null
)
