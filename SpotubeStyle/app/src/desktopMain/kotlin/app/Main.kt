package app

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import bridge.NewPipeResolver
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Desktop
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Plays through the system VLC install (vlcj). */
class VlcAudio : AudioPlayer {
    private val mp: MediaPlayer? = runCatching { MediaPlayerFactory("--no-video").mediaPlayers().newMediaPlayer() }.getOrNull()
    override var onEnded: (() -> Unit)? = null
    init { mp?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() { override fun finished(mediaPlayer: MediaPlayer) { onEnded?.invoke() } }) }
    override val available get() = mp != null
    override fun play(url: String) { mp?.media()?.play(url) }
    override fun pause() { mp?.controls()?.setPause(true) }
    override fun resume() { mp?.controls()?.setPause(false) }
    override fun seek(ms: Long) { mp?.controls()?.setTime(ms) }
    override fun setVolume(v: Float) { mp?.audio()?.setVolume((v * 100).toInt()) }
    override val positionMs get() = mp?.status()?.time() ?: 0L
    override val durationMs get() = mp?.status()?.length() ?: 0L
    override val isPlaying get() = mp?.status()?.isPlaying ?: false
}

class DesktopPlatform : Platform {
    private val file = File(System.getProperty("user.home"), ".spotube-style/settings.properties")
    private val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }
    override val isDesktop = true
    override val redirectUri = "http://127.0.0.1:8888/callback"
    override val player: AudioPlayer = VlcAudio()

    private fun browse(url: String) {
        val ok = runCatching { if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { Desktop.getDesktop().browse(URI(url)); true } else false }.getOrDefault(false)
        if (!ok) runCatching { ProcessBuilder("xdg-open", url).start() }
    }

    override suspend fun authorize(url: String): String? = withContext(Dispatchers.IO) {
        val done = CompletableFuture<String?>()
        val srv = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8888), 0)
        srv.createContext("/callback") { ex ->
            val code = (ex.requestURI.rawQuery ?: "").split("&").map { it.split("=", limit = 2) }
                .firstOrNull { it[0] == "code" }?.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") }
            val html = "<html><body style='font-family:sans-serif;background:#0b0b10;color:#f4f4f8;text-align:center;padding-top:20vh'><h2>${if (code != null) "Logged in - you can close this tab." else "Login failed."}</h2></body></html>".toByteArray()
            ex.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            ex.sendResponseHeaders(200, html.size.toLong()); ex.responseBody.use { it.write(html) }
            done.complete(code)
        }
        srv.start()
        try { browse(url); done.get(3, TimeUnit.MINUTES) } catch (e: Exception) { null } finally { srv.stop(0) }
    }

    override suspend fun resolveAudio(title: String, artist: String, durationMs: Long): String? =
        withContext(Dispatchers.IO) { NewPipeResolver.resolve(title, artist, durationMs) }
    override fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)
    override fun randomBytes(n: Int) = ByteArray(n).also { SecureRandom().nextBytes(it) }
    override fun nowMs() = System.currentTimeMillis()
    override fun load(key: String): String? = props.getProperty(key)
    override fun save(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
        file.parentFile.mkdirs(); file.outputStream().use { props.store(it, null) }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Spotube-style", state = rememberWindowState(width = 1200.dp, height = 780.dp)) {
        val scope = rememberCoroutineScope()
        val state = remember { AppState(DesktopPlatform(), scope) }
        App(state)
    }
}
