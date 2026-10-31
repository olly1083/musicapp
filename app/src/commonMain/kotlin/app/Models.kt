package app

data class Track(val id: String, val title: String, val artist: String, val album: String, val durationMs: Long, val art: String?)
data class Playlist(val id: String, val name: String, val art: String?)
data class LyricLine(val ms: Long, val text: String) // ms = -1 for unsynced lyrics
