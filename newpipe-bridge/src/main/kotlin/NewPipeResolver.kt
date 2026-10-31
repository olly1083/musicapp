package bridge

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val UA = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"

class OkDownloader : Downloader() {
    private val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val data = request.dataToSend()
        val body = when {
            data != null -> data.toRequestBody()
            method == "POST" -> ByteArray(0).toRequestBody()
            else -> null
        }
        val b = okhttp3.Request.Builder().url(request.url()).method(method, body).header("User-Agent", UA)
        request.headers().forEach { (k, vs) -> b.removeHeader(k); vs.forEach { v -> b.addHeader(k, v) } }
        client.newCall(b.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("rate limited", request.url())
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}

/** Finds the best matching YouTube Music song and returns a direct audio stream URL (NewPipe extractor). */
object NewPipeResolver {
    @Volatile private var ready = false
    private fun init() {
        if (!ready) synchronized(this) {
            if (!ready) { NewPipe.init(OkDownloader(), Localization("en", "US")); ready = true }
        }
    }

    fun resolve(title: String, artist: String, durationMs: Long): String? {
        init()
        val yt = ServiceList.YouTube
        val handler = yt.searchQHFactory.fromQuery(
            "$artist $title", listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), ""
        )
        val items = SearchInfo.getInfo(yt, handler).relatedItems.filterIsInstance<StreamInfoItem>().take(8)
        if (items.isEmpty()) return null
        val wanted = durationMs / 1000
        val best = items.minByOrNull { if (it.duration > 0 && wanted > 0) abs(it.duration - wanted) else 9999L }
            ?: items.first()
        val info = StreamInfo.getInfo(yt, best.url)
        val streams = info.audioStreams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val pick: AudioStream = streams.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
            ?: streams.maxByOrNull { it.averageBitrate } ?: return null
        return pick.content
    }
}
