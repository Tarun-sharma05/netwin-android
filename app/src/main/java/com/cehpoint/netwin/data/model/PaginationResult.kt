package com.cehpoint.netwin.data.model

data class PaginationResult<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val lastDocument: com.google.firebase.firestore.DocumentSnapshot?,
    val totalCount: Int = 0
)

data class PaginationParams(
    val pageSize: Int = 20,
    val lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null,
    val loadMore: Boolean = false
) 