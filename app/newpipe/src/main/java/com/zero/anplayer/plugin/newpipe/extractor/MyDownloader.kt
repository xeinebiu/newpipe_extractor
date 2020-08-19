package com.zero.anplayer.plugin.newpipe.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection


internal class MyDownloader private constructor() : Downloader() {

    private fun setDefaultHeaders(connection: URLConnection) {
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Language", DEFAULT_HTTP_ACCEPT_LANGUAGE)
    }

    override fun execute(request: Request): Response? {
        val httpMethod: String = request.httpMethod()
        val url: String = request.url()
        val headers: Map<String, List<String>> =
            request.headers()
        val dataToSend = request.dataToSend()
        // val localization = request.localization()
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = 30 * 1000 // 30s
        connection.readTimeout = 30 * 1000 // 30s
        connection.requestMethod = httpMethod
        setDefaultHeaders(connection)
        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                connection.setRequestProperty(headerName, null)
                for (headerValue in headerValueList) {
                    connection.addRequestProperty(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                connection.setRequestProperty(headerName, headerValueList[0])
            }
        }
        var outputStream: OutputStream? = null
        var input: InputStreamReader? = null
        return try {
            if (dataToSend != null && dataToSend.isNotEmpty()) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Length", dataToSend.size.toString() + "")
                outputStream = connection.outputStream
                outputStream.write(dataToSend)
            }
            val inputStream = connection.inputStream
            val response = StringBuilder()

            // Not passing any charset for decoding here... something to keep in mind.
            input = InputStreamReader(inputStream)
            var readCount: Int
            val buffer = CharArray(32 * 1024)
            while (input.read(buffer).also { readCount = it } != -1) {
                response.append(buffer, 0, readCount)
            }
            val responseCode: Int = connection.responseCode
            val responseMessage: String = connection.responseMessage
            val responseHeaders: Map<String, List<String>> =
                connection.headerFields
            val latestUrl: String = connection.url.toString()
            Response(
                responseCode,
                responseMessage,
                responseHeaders,
                response.toString(),
                latestUrl
            )
        } catch (e: Exception) {
            val responseCode: Int = connection.responseCode

            /*
                 * HTTP 429 == Too Many Request
                 * Receive from Youtube.com = ReCaptcha challenge request
                 * See : https://github.com/rg3/youtube-dl/issues/5138
                 */if (responseCode == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            } else if (responseCode != -1) {
                val latestUrl: String = connection.url.toString()
                return Response(
                    responseCode,
                    connection.responseMessage,
                    connection.headerFields,
                    null,
                    latestUrl
                )
            }
            throw IOException("Error occurred while fetching the content", e)
        } finally {
            outputStream?.close()
            input?.close()
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:68.0) Gecko/20100101 Firefox/68.0"
        private const val DEFAULT_HTTP_ACCEPT_LANGUAGE = "en"

        val instance by lazy {
            MyDownloader()
        }
    }
}
