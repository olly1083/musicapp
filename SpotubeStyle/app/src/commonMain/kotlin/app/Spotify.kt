package app

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
private data class TokenResp(val access_token: String, val refresh_token: String? = null, val expires_in: Int = 3600)

@OptIn(ExperimentalEncodingApi::class)
private fun b64(b: ByteArray): String = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(b)

private fun JsonElement?.str() = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.obj() = this as? JsonObject
private fun JsonElement?.arr() = this as? JsonArray

private const val SCOPES = "user-read-private user-library-read playlist-read-private playlist-read-collaborative"

/** Spotify metadata via the official Web API. Login = OAuth Authorization Code + PKCE in the system browser. */
class SpotifyClient(private val p: Platform) {
    private val http = HttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var token: String? = null
    private var expiresAt = 0L

    var clientId: String
        get() = p.load("client_id") ?: ""
        set(v) = p.save("client_id", v.trim())
    val loggedIn: Boolean get() = p.load("refresh") != null

    /** Returns an error message, or null on success. */
    suspend fun login(): String? {
        if (clientId.isBlank()) return "Enter your Spotify Client ID in Settings first."
        val verifier = b64(p.randomBytes(48))
        val challenge = b64(p.sha256(verifier.encodeToByteArray()))
        val url = "https://accounts.spotify.com/authorize?response_type=code" +
            "&client_id=${clientId.encodeURLParameter()}" +
            "&redirect_uri=${p.redirectUri.encodeURLParameter()}" +
            "&scope=${SCOPES.encodeURLParameter()}" +
            "&code_challenge_method=S256&code_challenge=$challenge"
        val code = p.authorize(url) ?: return "Login cancelled or timed out."
        return tokenRequest(
            "grant_type" to "authorization_code", "code" to code,
            "redirect_uri" to p.redirectUri, "client_id" to clientId, "code_verifier" to verifier
        )
    }

    fun logout() { token = null; p.save("refresh", null) }

    private suspend fun tokenRequest(vararg kv: Pair<String, String>): String? {
        val r = http.submitForm("https://accounts.spotify.com/api/token", parameters { kv.forEach { append(it.first, it.second) } })
        val body = r.bodyAsText()
        if (r.status.value !in 200..299) return "Spotify auth failed (${r.status.value}): ${body.take(120)}"
        val t = json.decodeFromString<TokenResp>(body)
        token = t.access_token
        expiresAt = p.nowMs() + (t.expires_in - 60) * 1000L
        t.refresh_token?.let { p.save("refresh", it) }
        return null
    }

    private suspend fun accessToken(): String? {
        if (token != null && p.nowMs() < expiresAt) return token
        val refresh = p.load("refresh") ?: return null
        val err = tokenRequest("grant_type" to "refresh_token", "refresh_token" to refresh, "client_id" to clientId)
        if (err != null) throw IllegalStateException(err)
        return token
    }

    private suspend fun get(path: String): JsonElement {
        val t = accessToken() ?: throw IllegalStateException("Not logged in")
        val r = http.get("https://api.spotify.com/v1$path") { header("Authorization", "Bearer $t") }
        val body = r.bodyAsText()
        if (r.status.value !in 200..299) throw IllegalStateException("Spotify ${r.status.value}: ${body.take(120)}")
        return json.parseToJsonElement(body)
    }

    private fun trackOf(e: JsonElement?): Track? {
        val o = e.obj() ?: return null
        val id = o["id"].str() ?: return null
        val alb = o["album"].obj()
        val artists = o["artists"].arr()?.joinToString(", ") { it.obj()?.get("name").str() ?: "" } ?: ""
        return Track(
            id, o["name"].str() ?: "Unknown", artists, alb?.get("name").str() ?: "",
            (o["duration_ms"] as? JsonPrimitive)?.longOrNull ?: 0L,
            alb?.get("images").arr()?.firstOrNull().obj()?.get("url").str()
        )
    }

    suspend fun me(): String = get("/me").obj()?.get("display_name").str() ?: "Spotify user"

    suspend fun playlists(): List<Playlist> =
        get("/me/playlists?limit=50").obj()?.get("items").arr()?.mapNotNull {
            val o = it.obj() ?: return@mapNotNull null
            Playlist(o["id"].str() ?: return@mapNotNull null, o["name"].str() ?: "", o["images"].arr()?.firstOrNull().obj()?.get("url").str())
        } ?: emptyList()

    suspend fun playlistTracks(id: String): List<Track> {
        val el = runCatching { get("/playlists/$id/tracks?limit=100") }.getOrElse { get("/playlists/$id/items?limit=100") }
        return el.obj()?.get("items").arr()?.mapNotNull { trackOf(it.obj()?.get("track") ?: it.obj()?.get("item")) } ?: emptyList()
    }

    suspend fun liked(): List<Track> =
        get("/me/tracks?limit=50").obj()?.get("items").arr()?.mapNotNull { trackOf(it.obj()?.get("track")) } ?: emptyList()

    suspend fun search(q: String): List<Track> =
        get("/search?type=track&limit=30&q=${q.encodeURLParameter()}").obj()?.get("tracks").obj()?.get("items").arr()?.mapNotNull { trackOf(it) } ?: emptyList()
}
