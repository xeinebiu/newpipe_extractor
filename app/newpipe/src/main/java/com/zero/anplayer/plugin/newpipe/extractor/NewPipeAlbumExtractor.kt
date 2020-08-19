package com.zero.anplayer.plugin.newpipe.extractor

import android.net.Uri
import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.AlbumExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Album
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.AuthorDescriptor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaDescriptor
import com.xeinebiu.utils.endpoint
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeAlbumExtractor(
    extractorCode: String,
    private val extractor: PlaylistExtractor
) : AlbumExtractor(extractorCode) {

    private var _album: Album? = null
    private var currentPage: ListExtractor.InfoItemsPage<StreamInfoItem>? = null

    init {
        extractor.fetchPage()
    }

    override fun getAlbum(): Album {
        var result = _album
        if (result == null) {
            val uri = Uri.parse(extractor.url)
            val authorUri = Uri.parse(extractor.uploaderUrl)
            result = Album(
                extractorCode,
                extractor.url,
                uri.endpoint(),
                extractor.name,
                "",
                Uri.parse(extractor.thumbnailUrl),
                Uri.parse(extractor.thumbnailUrl),
                Uri.parse(extractor.thumbnailUrl),
                uri,
                AuthorDescriptor(
                    extractorCode,
                    extractor.uploaderUrl,
                    authorUri.endpoint(),
                    extractor.uploaderName,
                    "",
                    Uri.parse(extractor.uploaderAvatarUrl),
                    Uri.parse(extractor.uploaderAvatarUrl),
                    Uri.parse(extractor.uploaderAvatarUrl),
                    authorUri
                ),
                extractor.streamCount.toInt()
            )
        }
        _album = result
        return result
    }

    override fun getFirstPage(): List<MediaDescriptor> {
        val page = extractor.initialPage
        currentPage = page

        return NewPipeUtils.toMediaDescriptors(extractorCode, page.items)
    }

    override fun getNextPage(): List<MediaDescriptor> =
        when {
            currentPage == null -> getFirstPage()
            hasNextPage() -> {
                val nextPage = extractor.getPage(currentPage!!.nextPage)
                currentPage = nextPage
                NewPipeUtils.toMediaDescriptors(extractorCode, nextPage.items)
            }
            else -> emptyList()
        }

    override fun hasNextPage(): Boolean =
        currentPage?.hasNextPage() == true
}