package com.zero.anplayer.plugin.newpipe.extractor

import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.FeedExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaDescriptor
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.kiosk.KioskExtractor

class NewPipeFeedExtractor(
    extractorCode: String,
    private val extractor: KioskExtractor<InfoItem>
) : FeedExtractor(extractorCode) {

    private var currentPage: ListExtractor.InfoItemsPage<InfoItem>? = null

    override fun getFirstPage(): List<MediaDescriptor> {
        extractor.fetchPage()

        val page = extractor.initialPage
        currentPage = page
        return NewPipeUtils.toMediaDescriptors(extractorCode, page.items)
    }

    override fun getNextPage(): List<MediaDescriptor> =
        when {
            currentPage == null -> getFirstPage()
            hasNextPage() -> {
                currentPage = extractor.getPage(currentPage!!.nextPage)
                NewPipeUtils.toMediaDescriptors(extractorCode, currentPage?.items)
            }
            else -> {
                emptyList()
            }
        }

    override fun hasNextPage(): Boolean =
        currentPage?.hasNextPage() == true
}