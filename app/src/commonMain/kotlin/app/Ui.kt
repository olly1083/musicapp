package app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

// Colours taken from the design board
object C {
    val bg = Color(0xFF0B0B10); val surface = Color(0xFF14141C); val card = Color(0xFF1B1B26)
    val hover = Color(0xFF1D1D28); val border = Color(0xFF262634); val accent = Color(0xFF6C5CE7)
    val pink = Color(0xFFE056A0); val mint = Color(0xFF7CF0C4); val text = Color(0xFFF4F4F8)
    val sub = Color(0xFFA9A9BD); val muted = Color(0xFF8A8AA0)
}

fun fmt(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

fun Tab.icon(): ImageVector = when (this) {
    Tab.Home -> Icons.Filled.Home; Tab.Search -> Icons.Filled.Search
    Tab.Library -> Icons.Filled.LibraryMusic; Tab.Settings -> Icons.Filled.Settings
}

@Composable
fun App(s: AppState) {
    MaterialTheme(colorScheme = darkColorScheme(primary = C.accent, background = C.bg, surface = C.surface, onSurface = C.text, onBackground = C.text)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(C.bg)) {
            if (maxWidth >= 820.dp) DesktopLayout(s) else MobileLayout(s)
        }
    }
}

// ---------- layouts ----------
@Composable
fun DesktopLayout(s: AppState) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(210.dp).fillMaxHeight().background(C.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Spotube-style", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            Tab.entries.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (s.tab == t) C.card else Color.Transparent)
                        .clickable { s.tab = t }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(t.icon(), null, tint = if (s.tab == t) C.mint else C.muted)
                    Spacer(Modifier.width(12.dp))
                    Text(t.name, color = if (s.tab == t) C.text else C.sub)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { Content(s) }
            PlayerBar(s)
        }
        if (s.current != null) {
            Column(Modifier.width(320.dp).fillMaxHeight().background(C.surface).padding(16.dp)) {
                Text("NOW PLAYING", color = C.muted, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                Art(s.current?.art, 288.dp, 14.dp)
                Spacer(Modifier.height(12.dp))
                Text(s.current?.title ?: "", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(s.current?.artist ?: "", color = C.sub, fontSize = 13.sp)
                Lyrics(s, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
fun MobileLayout(s: AppState) {
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { Content(s) }
            val t = s.current
            if (t != null) Row(
                Modifier.fillMaxWidth().background(C.card).clickable { s.showPlayer = true }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Art(t.art, 44.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(t.title, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(if (s.status.isNotBlank()) s.status else t.artist, color = C.sub, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = { s.toggle() }) { Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = C.text) }
            }
            NavigationBar(containerColor = C.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = s.tab == t, onClick = { s.tab = t },
                        icon = { Icon(t.icon(), null) }, label = { Text(t.name, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = C.mint, selectedTextColor = C.mint, indicatorColor = C.card, unselectedIconColor = C.muted, unselectedTextColor = C.muted)
                    )
                }
            }
        }
        if (s.showPlayer) FullPlayer(s)
    }
}

@Composable
fun FullPlayer(s: AppState) {
    val t = s.current ?: run { s.showPlayer = false; return }
    Column(Modifier.fillMaxSize().background(C.bg).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = { s.showPlayer = false }) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = C.text) } }
        Art(t.art, 240.dp, 16.dp)
        Spacer(Modifier.height(16.dp))
        Text(t.title, color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(t.artist, color = C.sub, fontSize = 14.sp, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        SeekBar(s)
        Transport(s, big = true)
        Lyrics(s, Modifier.weight(1f).fillMaxWidth())
    }
}

// ---------- player pieces ----------
@Composable
fun PlayerBar(s: AppState) {
    val t = s.current
    Row(Modifier.fillMaxWidth().height(84.dp).background(C.surface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (t != null) {
                Art(t.art, 52.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(t.title, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(if (s.status.isNotBlank()) s.status else t.artist, color = C.sub, fontSize = 12.sp, maxLines = 1)
                }
            } else Text(s.status.ifBlank { "Nothing playing" }, color = C.muted)
        }
        Column(Modifier.width(440.dp), horizontalAlignment = Alignment.CenterHorizontally) { Transport(s); SeekBar(s) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.VolumeUp, null, tint = C.muted)
            Slider(s.volume, { s.setVol(it) }, Modifier.width(130.dp), colors = SliderDefaults.colors(thumbColor = C.mint, activeTrackColor = C.mint, inactiveTrackColor = C.border))
        }
    }
}

@Composable
fun Transport(s: AppState, big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { s.prev() }) { Icon(Icons.Filled.SkipPrevious, null, tint = C.text) }
        FilledIconButton(
            onClick = { s.toggle() }, modifier = Modifier.size(if (big) 64.dp else 40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = C.accent)
        ) {
            if (s.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
        }
        IconButton(onClick = { s.next() }) { Icon(Icons.Filled.SkipNext, null, tint = C.text) }
    }
}

@Composable
fun SeekBar(s: AppState) {
    var drag by remember { mutableStateOf<Float?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(fmt(s.pos), color = C.muted, fontSize = 11.sp)
        Slider(
            value = drag ?: (if (s.dur > 0) (s.pos.toFloat() / s.dur).coerceIn(0f, 1f) else 0f),
            onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { s.seek((it * s.dur).toLong()) }; drag = null },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = C.mint, activeTrackColor = C.mint, inactiveTrackColor = C.border)
        )
        Text(fmt(s.dur), color = C.muted, fontSize = 11.sp)
    }
}

@Composable
fun Lyrics(s: AppState, modifier: Modifier) {
    val lines = s.lyrics
    if (lines.isEmpty()) {
        Box(modifier, Alignment.Center) { Text(if (s.lyricsLoading) "Loading lyrics…" else "No lyrics on LRCLIB", color = C.muted) }
        return
    }
    val active = lines.indexOfLast { it.ms in 0..s.pos }
    val st = rememberLazyListState()
    LaunchedEffect(active) { if (active >= 0) st.animateScrollToItem((active - 2).coerceAtLeast(0)) }
    LazyColumn(modifier, state = st, contentPadding = PaddingValues(vertical = 32.dp)) {
        itemsIndexed(lines) { i, l ->
            Text(
                l.text.ifBlank { "♪" },
                color = if (i == active) C.mint else if (active < 0) C.sub else C.muted,
                fontSize = if (i == active) 20.sp else 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun Art(url: String?, size: Dp, radius: Dp = 8.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(C.accent, C.pink)))) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun TrackRow(t: Track, active: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(t.art, 44.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(t.title, color = if (active) C.mint else C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(t.artist, color = C.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(fmt(t.durationMs), color = C.muted, fontSize = 12.sp)
    }
}

// ---------- screens ----------
@Composable
fun Content(s: AppState) = when (s.tab) {
    Tab.Home -> Home(s); Tab.Search -> SearchScreen(s); Tab.Library -> Library(s); Tab.Settings -> SettingsScreen(s)
}

@Composable
fun Heading(text: String) = Text(text, color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))

@Composable
fun Home(s: AppState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item { Text("Welcome back" + (s.user?.let { ", $it" } ?: ""), color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        if (!s.loggedIn) item {
            Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = C.card)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Connect Spotify", color = C.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Your browser opens, you log in, and your playlists + liked songs appear here. Audio comes from NewPipe, lyrics from LRCLIB.", color = C.sub, modifier = Modifier.padding(vertical = 8.dp))
                    if (s.clientId.isBlank()) Text("Add your Spotify Client ID in Settings first.", color = C.pink)
                    Button(onClick = { if (s.clientId.isBlank()) s.tab = Tab.Settings else s.login() }, colors = ButtonDefaults.buttonColors(containerColor = C.accent)) { Text("Log in with Spotify") }
                    if (s.status.isNotBlank()) Text(s.status, color = C.muted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            item { Heading("Your playlists") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(s.playlists) { pl ->
                        Column(Modifier.width(140.dp).clip(RoundedCornerShape(12.dp)).clickable { s.tab = Tab.Library; s.openPlaylist(pl) }.background(C.card).padding(10.dp)) {
                            Art(pl.art, 120.dp, 10.dp)
                            Text(pl.name, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            item { Heading("Liked songs") }
            itemsIndexed(s.liked) { i, t -> TrackRow(t, s.current?.id == t.id) { s.playList(s.liked, i) } }
            if (s.status.isNotBlank()) item { Text(s.status, color = C.pink, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
fun SearchScreen(s: AppState) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        OutlinedTextField(
            q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search songs on Spotify") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { s.search(q) }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.accent, unfocusedBorderColor = C.border, focusedTextColor = C.text, unfocusedTextColor = C.text)
        )
        if (!s.loggedIn) Text("Log in with Spotify in Settings to search.", color = C.muted, modifier = Modifier.padding(top = 12.dp))
        LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
            itemsIndexed(s.results) { i, t -> TrackRow(t, s.current?.id == t.id) { s.playList(s.results, i) } }
        }
    }
}

@Composable
fun Library(s: AppState) {
    val name = s.openedName
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        if (name != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { s.closeOpened() }) { Icon(Icons.Filled.ArrowBack, null, tint = C.text) }
                    Text(name, color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            itemsIndexed(s.openedTracks) { i, t -> TrackRow(t, s.current?.id == t.id) { s.playList(s.openedTracks, i) } }
        } else {
            item { Text("Your Library", color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            if (!s.loggedIn) item { Text("Log in with Spotify in Settings.", color = C.muted, modifier = Modifier.padding(top = 12.dp)) }
            else {
                item { LibRow("Liked Songs", null) { s.openLiked() } }
                items(s.playlists) { pl -> LibRow(pl.name, pl.art) { s.openPlaylist(pl) } }
            }
        }
    }
}

@Composable
fun LibRow(name: String, art: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(art, 52.dp); Text(name, color = C.text, modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsScreen(s: AppState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Heading("Spotify account") }
        item {
            OutlinedTextField(
                s.clientId, { s.saveClientId(it) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Spotify Client ID") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.accent, unfocusedBorderColor = C.border, focusedTextColor = C.text, unfocusedTextColor = C.text)
            )
        }
        item { Text("Add this Redirect URI in your Spotify developer dashboard:\n${s.p.redirectUri}", color = C.sub, fontSize = 13.sp) }
        item {
            if (s.loggedIn) Button(onClick = { s.logout() }, colors = ButtonDefaults.buttonColors(containerColor = C.card)) { Text("Log out${s.user?.let { " ($it)" } ?: ""}") }
            else Button(onClick = { s.login() }, colors = ButtonDefaults.buttonColors(containerColor = C.accent)) { Text("Log in with Spotify (opens browser)") }
        }
        if (s.status.isNotBlank()) item { Text(s.status, color = C.mint) }
        item { Heading("Sources") }
        item { Text("Audio: NewPipe extractor (YouTube Music)\nLyrics: LRCLIB (lrclib.net)", color = C.sub) }
        if (!s.p.player.available) item { Text("⚠ VLC was not found. Install VLC (64-bit) so audio can play.", color = C.pink) }
        item { Heading("Volume") }
        item { Slider(s.volume, { s.setVol(it) }, colors = SliderDefaults.colors(thumbColor = C.mint, activeTrackColor = C.mint, inactiveTrackColor = C.border)) }
    }
}
