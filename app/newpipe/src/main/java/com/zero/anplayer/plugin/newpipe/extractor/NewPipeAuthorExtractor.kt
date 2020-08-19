package com.zero.anplayer.plugin.newpipe.extractor

import android.net.Uri
import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.AuthorExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.AlbumCollection
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Author
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaTrackDescriptor
import com.xeinebiu.utils.endpoint
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeAuthorExtractor(
    extractorCode: String,
    private val extractor: ChannelExtractor
) : AuthorExtractor(extractorCode) {

    private var _author: Author? = null
    private var currentPage: ListExtractor.InfoItemsPage<StreamInfoItem>? = null

    init {
        extractor.fetchPage()
    }

    override fun getAuthor(): Author {
        var result = _author
        if (result == null) {
            val uri = Uri.parse(extractor.url)
            result = Author(
                extractorCode,
                extractor.url,
                uri.endpoint(),
                extractor.name,
                extractor.description,
                Uri.parse(extractor.avatarUrl),
                Uri.parse(extractor.avatarUrl),
                Uri.parse(extractor.avatarUrl),
                uri,
                arrayListOf(Uri.parse(extractor.bannerUrl)),
                emptyList()
            )
        }
        _author = result
        return result
    }


    override fun getFirstPage(): List<AlbumCollection> {
        val page = extractor.initialPage
        currentPage = page

        return arrayListOf(
            toAlbumCollection(
                NewPipeUtils.toMediaDescriptors(extractorCode, page.items)
                    .filterIsInstance<MediaTrackDescriptor>()
            )
        )
    }

    override fun getNextPage(): List<AlbumCollection> =
        when {
            currentPage == null -> getFirstPage()
            hasNextPage() -> {
                val nextPage = extractor.getPage(currentPage!!.nextPage)
                currentPage = nextPage
                arrayListOf(
                    toAlbumCollection(
                        NewPipeUtils.toMediaDescriptors(extractorCode, nextPage.items)
                            .filterIsInstance<MediaTrackDescriptor>()
                    )
                )
            }
            else -> emptyList()
        }

    override fun hasNextPage(): Boolean =
        currentPage?.hasNextPage() == true

    private fun toAlbumCollection(
        tracks: List<MediaTrackDescriptor>
    ): AlbumCollection {
        val uri = Uri.parse(extractor.url + "?album")
        return AlbumCollection(
            "Videos",
            emptyList(),
            tracks,
            uri.endpoint(),
            uri
        )
    }
}
