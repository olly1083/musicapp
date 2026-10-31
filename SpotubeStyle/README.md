# Spotube-style (Android · Windows · Linux)

Kotlin Multiplatform + Compose Multiplatform. One UI codebase, ported from your design board
(dark theme, desktop 3-pane layout + mobile bottom-nav layout).

| Part | How |
|---|---|
| Metadata | Spotify Web API. **Log in opens your browser** (OAuth + PKCE) |
| Audio | **NewPipeExtractor** (YouTube Music search -> best audio stream) |
| Lyrics | **LRCLIB** (synced LRC, falls back to plain) |
| Playback | Android: Media3 ExoPlayer. Desktop: VLC via vlcj (install VLC) |

## 1. Spotify setup (once)
1. https://developer.spotify.com/dashboard -> Create app.
2. Add **both** Redirect URIs:
   - `http://127.0.0.1:8888/callback`  (Windows/Linux)
   - `spotubestyle://callback`         (Android)
3. Copy the **Client ID** into the app: Settings -> Spotify Client ID -> Log in.

## 2. Build
Easiest: push to GitHub. `.github/workflows/build.yml` builds the APK, .deb and .msi for you.

Locally (JDK 17 + Android SDK for the APK):
    gradle wrapper            # once
    ./gradlew :app:run                 # run desktop app
    ./gradlew :app:assembleDebug       # Android APK
    ./gradlew :app:packageDeb          # Linux installer (packageMsi on Windows)

## Notes
- Spotube's plugin logs in with the web-player `sp_dc` cookie + TOTP. That can't be captured from a
  normal browser, so this app uses Spotify's official OAuth instead (same "browser opens, you log in" flow).
- Spotify dev-mode apps have restrictions (owner Premium, limited users). Check your dashboard.
- NewPipe is a JVM library, so there's no iOS build.
- Not included yet: Android background-playback service/notification, search filter chips.
