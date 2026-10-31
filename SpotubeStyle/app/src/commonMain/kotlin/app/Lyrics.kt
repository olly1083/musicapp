package app

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/** Lyrics from https://lrclib.net (synced LRC when available, plain text otherwise). */
class LrcLib {
    private val http = HttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val lrc = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?](.*)""")

    private suspend fun call(url: String): String? {
        val r = http.get(url) { header("User-Agent", "SpotubeStyle/0.1") }
        return if (r.status.value in 200..299) r.bodyAsText() else null
    }

    suspend fun fetch(t: Track): List<LyricLine> {
        val q = "track_name=${t.title.encodeURLParameter()}&artist_name=${t.artist.substringBefore(",").trim().encodeURLParameter()}" +
            "&album_name=${t.album.encodeURLParameter()}&duration=${t.durationMs / 1000}"
        var o: JsonObject? = call("https://lrclib.net/api/get?$q")?.let { json.parseToJsonElement(it) as? JsonObject }
        if (o == null) {
            val s = call("https://lrclib.net/api/search?q=${(t.artist.substringBefore(",") + " " + t.title).encodeURLParameter()}")
            o = (s?.let { json.parseToJsonElement(it) } as? JsonArray)?.firstOrNull { (it as? JsonObject)?.get("syncedLyrics") !is JsonNull }
                as? JsonObject
        }
        val synced = (o?.get("syncedLyrics") as? JsonPrimitive)?.contentOrNull
        if (!synced.isNullOrBlank()) {
            return synced.lines().mapNotNull { line ->
                val m = lrc.matchEntire(line.trim()) ?: return@mapNotNull null
                val frac = m.groupValues[3]
                val ms = m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1000 +
                    (if (frac.isEmpty()) 0 else (frac + "00").take(3).toLong())
                LyricLine(ms, m.groupValues[4].trim())
            }
        }
        val plain = (o?.get("plainLyrics") as? JsonPrimitive)?.contentOrNull
        return plain?.lines()?.map { LyricLine(-1, it) } ?: emptyList()
    }
}
