package com.example.search

data class SearchParams(
    val q: String = "",
    val filter: String? = null,
    val sort: List<String> = emptyList(),
    val page: Int = 1,
    val hitsPerPage: Int = 20,
    val facets: List<String> = emptyList()
)

data class SearchResult(
    val hits: List<Map<String, Any?>>,
    val query: String,
    val processingTimeMs: Long,
    val totalHits: Int?,
    val page: Int?,
    val hitsPerPage: Int?,
    val totalPages: Int?,
    val facetDistribution: Map<String, Map<String, Int>>?
)
