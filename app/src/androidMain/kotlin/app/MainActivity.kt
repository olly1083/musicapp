package app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import bridge.NewPipeResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.security.SecureRandom

class ExoAudio(ctx: Context) : AudioPlayer {
    private val ex = ExoPlayer.Builder(ctx)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        .setWakeMode(C.WAKE_MODE_NETWORK).build()
    override var onEnded: (() -> Unit)? = null
    init { ex.addListener(object : Player.Listener { override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) onEnded?.invoke() } }) }
    override val available = true
    override fun play(url: String) { ex.setMediaItem(MediaItem.fromUri(url)); ex.prepare(); ex.play() }
    override fun pause() = ex.pause()
    override fun resume() = ex.play()
    override fun seek(ms: Long) = ex.seekTo(ms)
    override fun setVolume(v: Float) { ex.volume = v }
    override val positionMs get() = ex.currentPosition
    override val durationMs get() = ex.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    override val isPlaying get() = ex.isPlaying
}

class AndroidPlatform(private val ctx: Context) : Platform {
    companion object { @Volatile var pending: CompletableDeferred<String?>? = null }
    private val prefs = ctx.getSharedPreferences("spotube_style", Context.MODE_PRIVATE)
    override val isDesktop = false
    override val redirectUri = "spotubestyle://callback"
    override val player: AudioPlayer = ExoAudio(ctx)
    override suspend fun authorize(url: String): String? {
        val d = CompletableDeferred<String?>(); pending = d
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return withTimeoutOrNull(180_000) { d.await() }
    }
    override suspend fun resolveAudio(title: String, artist: String, durationMs: Long): String? =
        withContext(Dispatchers.IO) { NewPipeResolver.resolve(title, artist, durationMs) }
    override fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)
    override fun randomBytes(n: Int) = ByteArray(n).also { SecureRandom().nextBytes(it) }
    override fun nowMs() = System.currentTimeMillis()
    override fun load(key: String): String? = prefs.getString(key, null)
    override fun save(key: String, value: String?) { prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val platform = AndroidPlatform(applicationContext)
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { AppState(platform, scope) }
            App(state)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val d = intent.data ?: return
        if (d.scheme == "spotubestyle") AndroidPlatform.pending?.complete(d.getQueryParameter("code"))
    }
}
