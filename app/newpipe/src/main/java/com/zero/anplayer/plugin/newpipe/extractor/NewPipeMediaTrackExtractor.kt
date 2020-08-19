package com.zero.anplayer.plugin.newpipe.extractor

import android.net.Uri
import com.xeinebiu.anplayer.plugin.base.media.extractor.enum.MediaFormat
import com.xeinebiu.anplayer.plugin.base.media.extractor.extractor.MediaTrackExtractor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Comment
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.MediaStream
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.MediaTrack
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.Subtitle
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.AuthorDescriptor
import com.xeinebiu.anplayer.plugin.base.media.extractor.model.descriptor.MediaTrackDescriptor
import com.xeinebiu.http.HttpClient
import com.xeinebiu.utils.MediaUtils
import com.xeinebiu.utils.endpoint
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.comments.CommentsExtractor
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.stream.StreamExtractor

class NewPipeMediaTrackExtractor(
    extractorCode: String,
    private val streamExtractor: StreamExtractor,
    private val commentsExtractor: CommentsExtractor
) : MediaTrackExtractor(extractorCode) {

    private var _subtitles: List<Subtitle>? = null
    private var _audioOnlyStreams: List<MediaStream>? = null
    private var _mixedStreams: List<MediaStream>? = null
    private var _videoOnlyStreams: List<MediaStream>? = null
    private var currentCommentsPage: ListExtractor.InfoItemsPage<CommentsInfoItem>? = null
    private var relatedTracks: List<MediaTrackDescriptor>? = null
    private var _track: MediaTrack? = null

    init {
        streamExtractor.fetchPage()
        commentsExtractor.fetchPage()
    }

    override fun getAudioOnlyStreams(): List<MediaStream> {
        var result = _audioOnlyStreams
        if (result == null) {
            result = streamExtractor.audioStreams?.map {
                val length = HttpClient.getContentLength(it.getUrl())
                val quality: Int = MediaStream.getQualityFromAudioBitrate(it.averageBitrate)
                val format = MediaFormat.getByName(it.getFormat().getName())
                NewPipeUtils.toMediaStream(
                    extractorCode,
                    format,
                    "0p",
                    quality,
                    length,
                    MediaStream.TYPE_MUSIC,
                    it.getUrl()
                )
            } ?: emptyList()
        }
        _audioOnlyStreams = result
        return result
    }

    override fun getCommentsFirstPage(): List<Comment> {
        val initialPage = commentsExtractor.initialPage
        currentCommentsPage = initialPage
        return initialPage.items?.map { NewPipeUtils.toComment(extractorCode, it) } ?: emptyList()
    }

    override fun getCommentsNextPage(): List<Comment> =
        when {
            currentCommentsPage == null -> getCommentsFirstPage()
            hasCommentsNextPage() -> {
                val nextPage = commentsExtractor.getPage(currentCommentsPage!!.nextPage)
                currentCommentsPage = nextPage
                nextPage.items?.map { NewPipeUtils.toComment(extractorCode, it) } ?: emptyList()
            }
            else -> {
                emptyList()
            }
        }

    override fun getMixedStreams(): List<MediaStream> {
        var result = _mixedStreams
        if (result == null) {
            result = streamExtractor.videoStreams?.map {
                val length = HttpClient.getContentLength(it.getUrl())
                val quality: Int = MediaStream.getQualityFromResolution(it.getResolution())
                val format = MediaFormat.getByName(it.getFormat().getName())
                NewPipeUtils.toMediaStream(
                    extractorCode,
                    format,
                    it.getResolution(),
                    quality,
                    length,
                    MediaStream.TYPE_MUSIC_VIDEO,
                    it.getUrl()
                )
            } ?: emptyList()
        }
        _mixedStreams = result
        return result
    }

    override fun getRelatedTracksFirstPage(): List<MediaTrackDescriptor> {
        var result = relatedTracks
        if (result == null) {
            result = streamExtractor.relatedStreams?.items?.map {
                NewPipeUtils.toMediaDescriptor(
                    extractorCode,
                    it
                )
            }?.filterIsInstance<MediaTrackDescriptor>() ?: emptyList()
        }
        relatedTracks = result
        return result
    }

    override fun getRelatedTracksNextPage(): List<MediaTrackDescriptor> =
        emptyList()

    override fun getSubtitles(): List<Subtitle> {
        var result = _subtitles
        if (result == null)
            result = streamExtractor.subtitlesDefault
                .map { NewPipeUtils.toSubtitle(extractorCode, it) }

        _subtitles = result
        return result
    }

    override fun getTrack(): MediaTrack {
        var result = _track
        if (result == null) {
            val uri = Uri.parse(streamExtractor.url)
            val authorUri = Uri.parse(streamExtractor.uploaderUrl)
            val averageRating =
                (streamExtractor.likeCount + streamExtractor.dislikeCount).toFloat() / 2
            result = MediaTrack(
                extractorCode,
                streamExtractor.url,
                uri.endpoint(),
                streamExtractor.name,
                streamExtractor.description.content,
                Uri.parse(streamExtractor.thumbnailUrl),
                Uri.parse(streamExtractor.thumbnailUrl),
                Uri.parse(streamExtractor.thumbnailUrl),
                uri,
                AuthorDescriptor(
                    extractorCode,
                    streamExtractor.uploaderUrl,
                    authorUri.endpoint(),
                    streamExtractor.uploaderName,
                    "",
                    Uri.parse(streamExtractor.uploaderAvatarUrl),
                    Uri.parse(streamExtractor.uploaderAvatarUrl),
                    Uri.parse(streamExtractor.uploaderAvatarUrl),
                    authorUri
                ),
                MediaUtils.readableMediaDuration(streamExtractor.length * 1000),
                true,
                streamExtractor.category,
                streamExtractor.tags,
                streamExtractor.textualUploadDate ?: "",
                streamExtractor.length * 1000,
                streamExtractor.viewCount,
                averageRating,
                streamExtractor.dislikeCount.toInt(),
                streamExtractor.likeCount.toInt()
            )
        }
        _track = result
        return result
    }

    override fun getVideoOnlyStreams(): List<MediaStream> {
        var result = _videoOnlyStreams
        if (result == null) {
            result = streamExtractor.videoOnlyStreams?.map {
                val length = HttpClient.getContentLength(it.getUrl())
                val quality: Int = MediaStream.getQualityFromResolution(it.getResolution())
                val format = MediaFormat.getByName(it.getFormat().getName())
                NewPipeUtils.toMediaStream(
                    extractorCode,
                    format,
                    it.getResolution(),
                    quality,
                    length,
                    MediaStream.TYPE_VIDEO,
                    it.getUrl()
                )
            } ?: emptyList()
        }
        _videoOnlyStreams = result
        return result
    }

    override fun hasCommentsNextPage(): Boolean =
        currentCommentsPage?.hasNextPage() == true

    override fun hasRelatedTracksNextPage(): Boolean = false
}
