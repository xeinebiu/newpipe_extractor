package com.zero.anplayer.plugin.newpipe.extractor

import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.AlbumExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Album
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaDescriptor

class NewPipeAuthorAlbumExtractor(
    extractorCode: String,
    private val extractor: NewPipeAuthorExtractor
) : AlbumExtractor(extractorCode) {

    private var _album: Album? = null
    override fun getAlbum(): Album {
        var result = _album
        if (result == null) {
            val author = extractor.getAuthor()
            result = Album(
                extractorCode,
                author.id,
                author.endpoint,
                author.name,
                author.description,
                author.smallThumbnail,
                author.mediumThumbnail,
                author.largeThumbnail,
                author.uri,
                author,
                -1
            )
        }
        _album = result
        return result
    }

    override fun getFirstPage(): List<MediaDescriptor> =
        extractor.getFirstPage().firstOrNull()?.tracks ?: emptyList()

    override fun getNextPage(): List<MediaDescriptor> =
        extractor.getNextPage().firstOrNull()?.tracks ?: emptyList()

    override fun hasNextPage(): Boolean =
        extractor.hasNextPage()
}