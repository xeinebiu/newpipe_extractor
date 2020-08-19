package com.zero.anplayer.plugin.newpipe

import android.net.Uri
import com.xeinebiu.anplayer.plugin.base.media.extractor.MediaExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.exception.NotImplementedExtractorException
import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.*
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Category
import com.zero.anplayer.plugin.newpipe.extractor.*
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.media_ccc.MediaCCCService
import org.schabi.newpipe.extractor.services.peertube.PeertubeService
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudService
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import java.net.CookieManager
import java.util.regex.Pattern

fun regex(text: String, pattern: String?): String? =
    if (pattern == null) null
    else {
        val p = Pattern.compile(pattern, Pattern.DOTALL)
        val m = p.matcher(text)
        m.find()
        m.group()
    }

class NewPipeExtractor : MediaExtractor() {
    private lateinit var service: StreamingService

    override fun init(cookieManager: CookieManager?, data: HashMap<String, String>?) {
        val serviceId = data?.get("service")?.toInt() ?: 0
        service = when (serviceId) {
            1 -> SoundcloudService(1)
            2 -> MediaCCCService(2)
            3 -> PeertubeService(3)
            else -> YoutubeService(0)
        }
        NewPipe.init(MyDownloader.instance, Localization("us", "en"))
    }

    override fun getAccountExtractor(): AccountExtractor =
        throw NotImplementedExtractorException(extractorCode)

    override fun getCategoryExtractor(category: Uri): CategoryExtractor =
        throw NotImplementedExtractorException(extractorCode)

    override fun getAlbumExtractor(album: Uri): AlbumExtractor =
        if (album.toString().endsWith("?album"))
            NewPipeAuthorAlbumExtractor(
                extractorCode,
                NewPipeAuthorExtractor(
                    extractorCode,
                    service.getChannelExtractor(
                        album.toString().replace("?album", "")
                    )
                )
            )
        else
            NewPipeAlbumExtractor(
                extractorCode,
                service.getPlaylistExtractor(album.toString())
            )

    override fun getAuthorExtractor(author: Uri): AuthorExtractor =
        NewPipeAuthorExtractor(
            extractorCode,
            service.getChannelExtractor(author.toString())
        )

    override fun getCategories(): List<Category> = emptyList()

    override fun getFeedExtractor(): FeedExtractor {
        val extractor = service.kioskList.defaultKioskExtractor
        return NewPipeFeedExtractor(extractorCode, extractor)
    }

    override fun getMediaTrackExtractor(track: Uri): MediaTrackExtractor {
        val streamExtractor = service.getStreamExtractor(track.toString())
        val commentExtractor = service.getCommentsExtractor(track.toString())
        return NewPipeMediaTrackExtractor(extractorCode, streamExtractor, commentExtractor)
    }

    override fun getSearchExtractor(): SearchExtractor =
        NewPipeSearchExtractor(
            extractorCode,
            service.suggestionExtractor
        ) {
            service.getSearchExtractor(it)
        }

    override fun isAlbum(uri: Uri): Boolean {
        val url = uri.toString()
        if (isSoundCloud(url))
            return url.contains("/sets/")

        if (isYoutube(url))
            return runCatching {
                !regex(
                    uri.toString(),
                    "https:.*playlist.list=.*"
                ).isNullOrBlank()
            }.getOrDefault(false)

        if (isMediaCcc(url))
            return runCatching { uri.pathSegments[0] == "c" }.getOrDefault(false)

        return false
    }

    override fun isAuthor(uri: Uri): Boolean {
        val url = uri.toString()
        if (isSoundCloud(url))
            return uri.pathSegments.size == 1

        if (isYoutube(url))
            return try {
                !regex(uri.toString(), "https:.*\\/user\\/.*").isNullOrBlank()
            } catch (e: Exception) {
                try {
                    !regex(uri.toString(), "https:.*\\/channel\\/.*").isNullOrBlank()
                } catch (e: Exception) {
                    false
                }
            }

        return false
    }

    override fun isMediaTrack(uri: Uri): Boolean {
        val url = uri.toString()
        if (isSoundCloud(url))
            return uri.pathSegments.size == 2

        if (isYoutube(url))
            return runCatching {
                !regex(
                    uri.toString(),
                    "https:.*watch\\?v=.*"
                ).isNullOrBlank()
            }.getOrDefault(false)

        if (isMediaCcc(url))
            return runCatching { uri.pathSegments[0] == "v" }.getOrDefault(false)

        return false
    }

    override fun isSupported(uri: Uri): Boolean {
        val url = uri.toString()
        return isSoundCloud(url)
    }

    private fun isSoundCloud(url: String): Boolean =
        url.startsWith(
            "https://www.soundcloud.",
            true
        ) || url.startsWith("https://soundcloud.", true)

    private fun isYoutube(url: String): Boolean =
        url.startsWith(
            "https://www.youtube.",
            true
        ) || url.startsWith("https://m.youtube.", true)

    private fun isMediaCcc(url: String): Boolean =
        url.startsWith(
            "https://media.ccc.de",
            true
        )
}
