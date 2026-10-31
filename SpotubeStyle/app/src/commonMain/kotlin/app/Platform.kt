package app

interface AudioPlayer {
    val available: Boolean
    fun play(url: String)
    fun pause()
    fun resume()
    fun seek(ms: Long)
    fun setVolume(v: Float)
    val positionMs: Long
    val durationMs: Long
    val isPlaying: Boolean
    var onEnded: (() -> Unit)?
}

/** Everything that differs between Android and Desktop. */
interface Platform {
    val isDesktop: Boolean
    val redirectUri: String
    val player: AudioPlayer
    /** Opens the system browser at [url] and returns the OAuth `code` once Spotify redirects back (null = cancelled/timeout). */
    suspend fun authorize(url: String): String?
    /** NewPipe: find + resolve a direct audio stream URL. */
    suspend fun resolveAudio(title: String, artist: String, durationMs: Long): String?
    fun sha256(b: ByteArray): ByteArray
    fun randomBytes(n: Int): ByteArray
    fun nowMs(): Long
    fun load(key: String): String?
    fun save(key: String, value: String?)
}
