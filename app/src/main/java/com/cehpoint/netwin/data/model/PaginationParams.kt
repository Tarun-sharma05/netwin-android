package com.cehpoint.netwin.data.model

data class PaginationParams(
    val pageSize: Int,
    val lastDocument: String? = null,
    val loadMore: Boolean = false
)
