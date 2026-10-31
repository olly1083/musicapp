package app

import androidx.compose.runtime.*
import kotlinx.coroutines.*

enum class Tab { Home, Search, Library, Settings }

class AppState(val p: Platform, private val scope: CoroutineScope) {
    val spotify = SpotifyClient(p)
    private val lrc = LrcLib()

    var tab by mutableStateOf(Tab.Home)
    var showPlayer by mutableStateOf(false)
    var loggedIn by mutableStateOf(spotify.loggedIn)
    var user by mutableStateOf<String?>(null)
    var status by mutableStateOf("")
    var clientId by mutableStateOf(spotify.clientId)

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
    var liked by mutableStateOf<List<Track>>(emptyList())
    var results by mutableStateOf<List<Track>>(emptyList())
    var openedName by mutableStateOf<String?>(null)
    var openedTracks by mutableStateOf<List<Track>>(emptyList())

    var queue by mutableStateOf<List<Track>>(emptyList())
    var index by mutableIntStateOf(-1)
    val current: Track? get() = queue.getOrNull(index)
    var playing by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var pos by mutableLongStateOf(0L)
    var dur by mutableLongStateOf(0L)
    var volume by mutableFloatStateOf(1f)
    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsLoading by mutableStateOf(false)

    init {
        p.player.onEnded = { scope.launch { next() } }
        scope.launch {
            while (true) {
                delay(250)
                pos = p.player.positionMs; dur = p.player.durationMs; playing = p.player.isPlaying
            }
        }
        if (loggedIn) refresh()
    }

    fun saveClientId(v: String) { clientId = v; spotify.clientId = v }

    fun login() { scope.launch {
        status = "Opening your browser…"
        val err = spotify.login()
        loggedIn = spotify.loggedIn
        status = err ?: "Logged in"
        if (err == null) refresh()
    } }

    fun logout() { spotify.logout(); loggedIn = false; user = null; playlists = emptyList(); liked = emptyList(); status = "Logged out" }

    fun refresh() { scope.launch {
        runCatching { user = spotify.me(); playlists = spotify.playlists(); liked = spotify.liked() }
            .onFailure { status = it.message ?: "Spotify error" }
    } }

    fun search(q: String) { if (q.isBlank()) return; scope.launch {
        runCatching { results = spotify.search(q) }.onFailure { status = it.message ?: "Search failed" }
    } }

    fun openPlaylist(pl: Playlist) { openedName = pl.name; openedTracks = emptyList(); scope.launch {
        runCatching { openedTracks = spotify.playlistTracks(pl.id) }.onFailure { status = it.message ?: "Failed to load playlist" }
    } }
    fun openLiked() { openedName = "Liked Songs"; openedTracks = liked }
    fun closeOpened() { openedName = null }

    fun playList(list: List<Track>, i: Int) { queue = list; index = i; startCurrent() }

    private fun startCurrent() {
        val t = current ?: return
        loading = true; lyrics = emptyList(); lyricsLoading = true; status = "Finding audio…"
        scope.launch {
            val url = runCatching { p.resolveAudio(t.title, t.artist, t.durationMs) }
                .onFailure { status = "Audio lookup failed: ${it.message}" }.getOrNull()
            if (current?.id != t.id) return@launch
            loading = false
            if (url == null) { if (!status.startsWith("Audio lookup")) status = "No audio found for this track"; return@launch }
            status = ""
            p.player.setVolume(volume)
            p.player.play(url)
        }
        scope.launch {
            val l = runCatching { lrc.fetch(t) }.getOrDefault(emptyList())
            if (current?.id == t.id) { lyrics = l; lyricsLoading = false }
        }
    }

    fun toggle() { if (current == null) return; if (p.player.isPlaying) p.player.pause() else p.player.resume() }
    fun next() { if (index + 1 < queue.size) { index++; startCurrent() } }
    fun prev() { if (pos > 3000) p.player.seek(0) else if (index > 0) { index--; startCurrent() } }
    fun seek(ms: Long) = p.player.seek(ms)
    fun setVol(v: Float) { volume = v; p.player.setVolume(v) }
}
