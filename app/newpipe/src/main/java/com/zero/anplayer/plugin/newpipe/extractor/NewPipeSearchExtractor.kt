package com.zero.anplayer.plugin.newpipe.extractor

import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.SearchExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaDescriptor
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor

class NewPipeSearchExtractor(
    extractorCode: String,
    private val suggestionExtractor: SuggestionExtractor,
    private val searchExtractor: (query: String) -> org.schabi.newpipe.extractor.search.SearchExtractor
) : SearchExtractor(extractorCode) {

    private var currentExtractor: org.schabi.newpipe.extractor.search.SearchExtractor? = null
    private var currentPage: ListExtractor.InfoItemsPage<InfoItem>? = null

    override fun getQuery(): String? =
        currentExtractor?.searchString

    override fun getAutoCompletedWords(query: String): List<String> =
        suggestionExtractor.suggestionList(query)?.mapNotNull { it } ?: emptyList()

    override fun search(query: String): List<MediaDescriptor> {
        currentExtractor = searchExtractor(query).also { it.fetchPage() }
        currentPage = currentExtractor!!.initialPage
        return currentPage?.items?.mapNotNull {
            NewPipeUtils.toMediaDescriptor(extractorCode, it)
        } ?: emptyList()
    }

    override fun getNextPage(): List<MediaDescriptor> =
        if (hasNextPage()) {
            currentPage = currentExtractor!!.getPage(currentPage!!.nextPage)
            currentPage?.items?.mapNotNull {
                NewPipeUtils.toMediaDescriptor(extractorCode, it)
            } ?: emptyList()
        } else
            emptyList()

    override fun hasNextPage(): Boolean =
        currentPage?.hasNextPage() == true
}