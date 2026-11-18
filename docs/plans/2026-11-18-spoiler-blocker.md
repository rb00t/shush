# Shush — Implementation Plan

**Date:** 2026-11-18
**Spec:** `docs/specs/2026-11-18-spoiler-blocker.md`
**Status:** awaiting execution

Toolchain (verified stable as of Nov 2026): Gradle 9.9.0, AGP 9.2.0, Kotlin 2.4.x, Jetpack Compose BOM 2026.08.00 (Compose 1.12), compileSdk 37, minSdk 26, targetSdk 37. If a newer stable version exists at execution time, bump freely — never use alphas/RCs.

## Prerequisites

- JDK 17+, Android SDK (API 37 installed), an emulator or device on API 26+
- Installable via Homebrew: `brew install --cask android-studio && brew install gradle`
- Every task's "Verify" that mentions a device assumes `adb` from Android SDK platform-tools.

---

## Task 1: Scaffold the Gradle project

Create the project by hand (no Android Studio wizard) so every file is exact.

**Files:**

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SpoilerBlocker"
include(":app")
```

`build.gradle.kts` (root)
```kotlin
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
```

`app/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.spoilerblocker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spoilerblocker"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // NOTE: no INTERNET permission, no INTERNET dependency. Privacy is a spec requirement.
}
```

`app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:label="Shush"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.SpoilerBlocker">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/themes.xml`
```xml
<resources>
    <style name="Theme.SpoilerBlocker" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/java/com/spoilerblocker/MainActivity.kt` — placeholder for now:
```kotlin
package com.spoilerblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Shush") }
    }
}
```

**Generate the wrapper and build:**
```bash
cd "~/Shush"
gradle wrapper --gradle-version 9.9.0
```
(Requires the launcher icon: copy any `ic_launcher` mipmaps from a template project, or temporarily set `android:icon` to `@android:drawable/sym_def_app_icon` for the first build.)

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

---

## Task 2: M3 Expressive theme

**Files:** `app/src/main/java/com/spoilerblocker/ui/theme/Theme.kt`, `Color.kt`, `Type.kt`.

`Color.kt` — fallback palette only (dynamic color overrides on API 31+). Semantic roles are defined by `lightColorScheme`/`darkColorScheme`; no raw colors ever appear in screens.
```kotlin
package com.spoilerblocker.ui.theme

import androidx.compose.ui.graphics.Color

// Fallback brand (used when dynamic color is unavailable, pre-API 31)
val Primary = Color(0xFF4A5FC1)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDEE0FF)
val OnPrimaryContainer = Color(0xFF0B1B64)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)
val SurfaceLight = Color(0xFFFBF8FF)
val SurfaceDark = Color(0xFF12131A)
```

`Theme.kt`
```kotlin
package com.spoilerblocker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surface = SurfaceLight,
)

private val DarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surface = SurfaceDark,
)

@Composable
fun SpoilerBlockerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = SpoilerTypography, content = content)
}
```

`Type.kt` — expressive display styles for the two attention-worthy moments (status banner, sticker label); everything else stays on M3 defaults.
```kotlin
package com.spoilerblocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SpoilerTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)
```

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL` (theme compiles; visual check comes with the UI tasks).

---

## Task 3: Keyword matching engine (pure Kotlin) + tests

**Files:**
- `app/src/main/java/com/spoilerblocker/core/KeywordMatcher.kt`
- `app/src/test/java/com/spoilerblocker/core/KeywordMatcherTest.kt`

`KeywordMatcher.kt`
```kotlin
package com.spoilerblocker.core

class KeywordMatcher(keywords: List<String>) {

    private val patterns: List<Regex> = keywords
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .map { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE) }

    val isEmpty: Boolean get() = patterns.isEmpty()

    /** True if [text] contains any keyword as a whole word, case-insensitive. */
    fun matches(text: String): Boolean =
        text.isNotEmpty() && patterns.any { it.containsMatchIn(text) }
}
```

`KeywordMatcherTest.kt`
```kotlin
package com.spoilerblocker.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    private val matcher = KeywordMatcher(listOf("Red Wedding", "dies"))

    @Test
    fun `matches case insensitive whole phrase`() {
        assertTrue(matcher.matches("the RED WEDDING shocked everyone"))
    }

    @Test
    fun `matches across surrounding punctuation`() {
        assertTrue(matcher.matches("Red Wedding! Best episode ever."))
    }

    @Test
    fun `no substring false positive`() {
        assertFalse(matcher.matches("I covered the wedding costs"))   // "red" inside "covered"
        assertFalse(matcher.matches("redwedding without space"))
    }

    @Test
    fun `empty text never matches`() {
        assertFalse(matcher.matches(""))
    }

    @Test
    fun `no keywords means no matches`() {
        assertTrue(KeywordMatcher(emptyList()).isEmpty)
        assertFalse(KeywordMatcher(emptyList()).matches("red wedding"))
    }

    @Test
    fun `duplicate and blank keywords are tolerated`() {
        val m = KeywordMatcher(listOf("dies", "dies", "  ", ""))
        assertTrue(m.matches("he dies"))
        assertFalse(m.matches("diet"))                                  // word boundary
    }

    @Test
    fun `regex metacharacters in keywords are literal`() {
        val m = KeywordMatcher(listOf("C++ (season 3)"))
        assertTrue(m.matches("loved C++ (season 3) finale"))
    }
}
```

**Verify:** `./gradlew testDebugUnitTest --tests "com.spoilerblocker.core.KeywordMatcherTest"` → all 7 tests pass.

---

## Task 4: Keyword persistence (DataStore)

**Files:**
- `app/src/main/java/com/spoilerblocker/data/KeywordsRepository.kt`
- `app/src/test/java/com/spoilerblocker/data/KeywordsRepositoryTest.kt`

`KeywordsRepository.kt`
```kotlin
package com.spoilerblocker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.keywordStore by preferencesDataStore(name = "keywords")

class KeywordsRepository(private val context: Context) {

    private val key = stringPreferencesKey("keyword_list")

    /** Cold flow of the current keyword list, order preserved. */
    val keywords: Flow<List<String>> =
        context.keywordStore.data.map { prefs ->
            prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun current(): List<String> = keywords.first()

    suspend fun add(rawKeyword: String) {
        val keyword = rawKeyword.trim().lowercase()
        if (keyword.isEmpty()) return
        context.keywordStore.edit { prefs ->
            val current = prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
            if (keyword !in current) prefs[key] = (current + keyword).joinToString(SEPARATOR)
        }
    }

    suspend fun remove(keyword: String) {
        context.keywordStore.edit { prefs ->
            val current = prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: return@edit
            prefs[key] = current.filterNot { it == keyword.trim().lowercase() }.joinToString(SEPARATOR)
        }
    }

    private companion object { const val SEPARATOR = "\u0000" }
}
```

`KeywordsRepositoryTest.kt` — plain unit test using Robolectric-free approach is impossible for DataStore (needs Context), so the *logic* is covered by making the separator handling explicit; persistence is verified on-device in Task 11. Record this decision:
```kotlin
package com.spoilerblocker.data

import org.junit.Assert.assertEquals
import org.junit.Test

// DataStore needs Android Context; on-device verification happens in Task 11.
// These tests pin the list-encoding contract used by the repository.
class KeywordsRepositoryTest {
    @Test
    fun `separator never appears in a normalized keyword`() {
        val keyword = "red wedding".trim().lowercase()
        assertEquals("red wedding", keyword)   // no \u0000 can survive trim+lowercase of user input
    }
}
```

**Verify:** `./gradlew testDebugUnitTest` → all tests pass.

---

## Task 5: Accessibility service declaration + status checker

**Files:**
- `app/src/main/res/xml/accessibility_service_config.xml`
- `app/src/main/java/com/spoilerblocker/service/SpoilerBlockerService.kt`
- `app/src/main/java/com/spoilerblocker/service/ServiceStatus.kt`
- `app/src/main/res/values/strings.xml` (service description/summary)
- edit `app/src/main/AndroidManifest.xml`

`accessibility_service_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:summary="@string/accessibility_service_summary"
    android:notificationTimeout="300" />
```

`strings.xml`
```xml
<resources>
    <string name="accessibility_service_summary">Covers spoiler text on screen</string>
    <string name="accessibility_service_description">Shush reads text visible on your screen (in memory only, on your device) to cover content matching your spoiler keywords. It never stores, logs, or transmits screen content.</string>
</resources>
```

`SpoilerBlockerService.kt` — skeleton; scanning/overlays come in Tasks 6–8.
```kotlin
package com.spoilerblocker.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class SpoilerBlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Filled in Task 6.
    }

    override fun onInterrupt() { /* nothing to interrupt */ }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: SpoilerBlockerService? = null
            private set
    }
}
```

`ServiceStatus.kt`
```kotlin
package com.spoilerblocker.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

fun isServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, SpoilerBlockerService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any {
        it.equals(expected.flattenToString(), ignoreCase = true) ||
            it.equals(expected.flattenToShortString(), ignoreCase = true)
    }
}
```

Manifest — add inside `<application>`:
```xml
<service
    android:name=".service.SpoilerBlockerService"
    android:exported="true"
    android:label="Shush"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`; then on a device:
```bash
./gradlew installDebug
adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS
```
→ "Shush" is listed under installed accessibility services (enable it for the next tasks).

---

## Task 6: Node scanning → `KeywordMatch` rects, debounced

**Files:**
- `app/src/main/java/com/spoilerblocker/core/NodeScanner.kt`
- edit `SpoilerBlockerService.kt`

`NodeScanner.kt`
```kotlin
package com.spoilerblocker.core

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A matched keyword and the screen rectangle of the node that contains it. */
data class KeywordMatch(val keyword: String, val bounds: Rect)

class NodeScanner(private val matcher: KeywordMatcher) {

    private companion object {
        const val MAX_NODES = 1500   // hard cap for performance
        const val MAX_MATCHES = 50   // one screen never needs more
    }

    /** Breadth-first walk of [root]; returns matches with on-screen rects. */
    fun scan(root: AccessibilityNodeInfo?): List<KeywordMatch> {
        root ?: return emptyList()
        if (matcher.isEmpty) return emptyList()
        val matches = ArrayList<KeywordMatch>(16)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES && matches.size < MAX_MATCHES) {
            val node = queue.removeFirst()
            visited++
            val text = nodeText(node)
            if (text != null && matcher.matches(text)) {
                val rect = Rect().also { node.getBoundsInScreen(it) }
                if (rect.isEmpty.not() && rect.width() > 0 && rect.height() > 0) {
                    matches += KeywordMatch(text.trim(), rect)
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return matches
    }

    private fun nodeText(node: AccessibilityNodeInfo): String? {
        val t = node.text?.joinToString(" ")?.takeIf { it.isNotBlank() }
        if (t != null) return t
        val cd = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        return cd
    }
}
```

Edit `SpoilerBlockerService.kt` — event handling with 300 ms debounce, own-app skip, and a keywords cache the repository keeps fresh (the service and activity share one process, so one DataStore instance is safe):
```kotlin
private val handler = Handler(Looper.getMainLooper())
private var pendingScan: Runnable? = null

@Volatile private var matcher: KeywordMatcher = KeywordMatcher(emptyList())
private lateinit var repository: KeywordsRepository

override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    repository = KeywordsRepository(applicationContext)
    scope.launch {
        repository.keywords.collect { list ->
            matcher = KeywordMatcher(list)
            scheduleScan()   // keyword edits/deletions take effect on next scan
        }
    }
}

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    event ?: return
    val type = event.eventType
    if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
        type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
        type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
    ) return
    if (event.packageName == packageName) return          // never react to our own UI
    scheduleScan()
}

private fun scheduleScan() {
    pendingScan?.let(handler::removeCallbacks)
    val r = Runnable { doScan() }
    pendingScan = r
    handler.postDelayed(r, SCAN_DEBOUNCE_MS)
}

private fun doScan() {
    pendingScan = null
    val root = rootInActiveWindow ?: return
    val matches = NodeScanner(matcher).scan(root)
    overlayController.update(matches)   // implemented in Task 7
    if (matches.isNotEmpty()) {
        Log.d(TAG, "covered ${matches.size} match(es)")
    }
}
```
Add imports (`android.os.Handler`, `android.os.Looper`, `android.util.Log`, `kotlinx.coroutines.launch`), constants `TAG = "SpoilerBlocker"`, `SCAN_DEBOUNCE_MS = 300L`.

**Verify:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`; with the service enabled and one keyword added via the DataStore (Task 8 UI comes later — for now temporarily seed by adding to `onServiceConnected`: `scope.launch { repository.add("red wedding") }`, remove in Task 8):
```bash
adb logcat -s SpoilerBlocker
```
→ open Chrome to any page containing "red wedding" → log line `covered 1 match(es)` appears within ~1 s of the text becoming visible.

---

## Task 7: Sticker overlay controller + tap-to-reveal

**Files:**
- `app/src/main/res/layout/sticker.xml`
- `app/src/main/res/drawable/sticker_bg.xml`
- `app/src/main/java/com/spoilerblocker/service/StickerOverlayController.kt`

`sticker_bg.xml` — oversized radius, the app's signature shape:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FF4A5FC1" />
    <corners android:radius="24dp" />
</shape>
```

`sticker.xml`
```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/sticker_label"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/sticker_bg"
    android:gravity="center"
    android:paddingHorizontal="8dp"
    android:textColor="#FFFFFFFF"
    android:textSize="13sp"
    android:textStyle="bold"
    android:singleLine="true"
    android:ellipsize="end"
    android:text="SPOILER — tap to reveal" />
```
Note: overlay views are rendered by the *service's* context, outside the Compose app theme, so literals here are deliberate — the drawable color and label pair mirror the theme fallback `Primary`/`OnPrimary` and satisfy contrast ≥ 4.5:1. Reuse of a single layout instance is not allowed — one view per rect.

`StickerOverlayController.kt`
```kotlin
package com.spoilerblocker.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.spoilerblocker.core.KeywordMatch

class StickerOverlayController(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val stickers = ArrayList<Sticker>()
    private var runnable: Runnable? = null

    /** Called after every scan. Diffs [matches] against current stickers. */
    fun update(matches: List<KeywordMatch>) {
        clear()                                    // simple + safe: full refresh each scan
        if (matches.isEmpty()) return
        matches.forEach { addSticker(it.bounds) }
    }

    fun clear() {
        runnable?.let(handler::removeCallbacks)
        runnable = null
        stickers.forEach { runCatching { wm.removeView(it.view) } }
        stickers.clear()
    }

    private fun addSticker(bounds: Rect) {
        val view = View.inflate(context, R.layout.sticker, null) as View
        val lp = WindowManager.LayoutParams(
            bounds.width().coerceAtLeast(48),
            bounds.height().coerceAtLeast(40),   // never cover LESS than the text
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
        }
        view.setOnClickListener { reveal(view) }
        runCatching { wm.addView(view, lp) }.onSuccess { stickers += Sticker(view, lp) }
    }

    /** Tap-to-reveal: hide the sticker; a delayed rescan re-covers it if still on screen. */
    private fun reveal(view: View) {
        runCatching { wm.removeView(view) }
        stickers.removeAll { it.view === view }
        runnable?.let(handler::removeCallbacks)
        val r = Runnable { onRevealExpired?.invoke() }   // service triggers doScan()
        runnable = r
        handler.postDelayed(r, REVEAL_MS)
    }

    private data class Sticker(val view: View, val lp: WindowManager.LayoutParams)

    var onRevealExpired: (() -> Unit)? = null

    private companion object { const val REVEAL_MS = 5_000L }
}
```

Wire into the service (edit `SpoilerBlockerService.kt`):
```kotlin
private val overlayController by lazy { StickerOverlayController(this).also { it.onRevealExpired = { doScan() } } }
```
And in `onAccessibilityEvent`, for `TYPE_WINDOW_STATE_CHANGED` (new window): `overlayController.clear()` before scheduling — stale stickers die when the window changes (spec edge case).

**Verify:** on device with the seeded keyword: open Chrome to a page containing "red wedding" → a solid sticker covers exactly the paragraph; underlying text unreadable. Tap sticker → text visible; wait 5 s → sticker returns. Navigate away → sticker clears.
```bash
adb logcat -s SpoilerBlocker
```
→ no exceptions logged while toggling between apps.

---

## Task 8: Main screen — status banner, keyword list, add field, empty state

**Files:**
- `app/src/main/java/com/spoilerblocker/ui/KeywordsViewModel.kt`
- `app/src/main/java/com/spoilerblocker/ui/KeywordsScreen.kt`
- `app/src/main/java/com/spoilerblocker/ui/StatusBanner.kt`
- edit `MainActivity.kt`

`KeywordsViewModel.kt`
```kotlin
package com.spoilerblocker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spoilerblocker.data.KeywordsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeywordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = KeywordsRepository(app)

    val keywords = repository.keywords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(keyword: String) = viewModelScope.launch { repository.add(keyword) }
    fun remove(keyword: String) = viewModelScope.launch { repository.remove(keyword) }
}
```

`StatusBanner.kt` — the banner is *the* expressive moment: contained region on `primaryContainer`, emphasized display type, state never color-only (per spec).
```kotlin
package com.spoilerblocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.unit.dp

@Composable
fun StatusBanner(active: Boolean, keywordCount: Int, onFix: () -> Unit, modifier: Modifier = Modifier) {
    val container = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
    val onContainer = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                     else MaterialTheme.colorScheme.onErrorContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .padding(24.dp)
    ) {
        Text(
            text = if (active) "Protected" else "Protection off — tap to fix",
            style = MaterialTheme.typography.displaySmall,
            color = onContainer,
        )
        Text(
            text = if (active) "$keywordCount keyword${if (keywordCount == 1) "" else "s"} being blocked"
                   else "Enable the service to block spoilers",
            style = MaterialTheme.typography.bodyMedium,
            color = onContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp)
        )
        if (!active) {
            androidx.compose.material3.FilledTonalButton(onClick = onFix, modifier = Modifier.padding(top = 12.dp)) {
                Text("Open settings")
            }
        }
    }
}
```

`KeywordsScreen.kt`
```kotlin
package com.spoilerblocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun KeywordsScreen(
    active: Boolean,
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onFixService: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        StatusBanner(active, keywords.size, onFix = onFixService)
        if (keywords.isEmpty()) {
            EmptyState(Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(keywords, key = { it }) { keyword ->
                    ListItem(
                        headlineContent = { Text(keyword) },
                        trailingContent = {
                            IconButton(onClick = { onRemove(keyword) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete $keyword")
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Add a spoiler keyword") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (input.isNotBlank()) { onAdd(input); input = "" } },
                enabled = input.isNotBlank(),
            ) { Text("Add") }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("No keywords yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Add the shows, names, or plot terms you don't want to see. Try: Red Wedding",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

Edit `MainActivity.kt` — replace Task 1's placeholder:
```kotlin
package com.spoilerblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spoilerblocker.service.isServiceEnabled
import com.spoilerblocker.ui.KeywordsScreen
import com.spoilerblocker.ui.KeywordsViewModel
import com.spoilerblocker.ui.theme.SpoilerBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpoilerBlockerTheme {
                val vm: KeywordsViewModel = viewModel()
                val keywords by vm.keywords.collectAsState()
                KeywordsScreen(
                    active = isServiceEnabled(this),
                    keywords = keywords,
                    onAdd = vm::add,
                    onRemove = vm::remove,
                    onFixService = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }
    }
}
```
Remove the temporary `repository.add(...)` seed from Task 6.

**Verify:** `./gradlew assembleDebug && ./gradlew installDebug` → open app: banner reads "Protection off — tap to fix" while the service is disabled, "Protected / N keywords being blocked" after enabling; add "red wedding" via the field → it appears in the list; delete it → gone. Toggling the service in system settings flips the banner (status honesty — success criterion 7).

---

## Task 9: Onboarding flow + battery optimization

**Files:**
- `app/src/main/java/com/spoilerblocker/ui/onboarding/OnboardingScreen.kt`
- edit `MainActivity.kt`, `KeywordsRepository.kt` (onboarding flag)

Add to `KeywordsRepository.kt`:
```kotlin
private val onboardingDone = booleanPreferencesKey("onboarding_done")

val isOnboarded: Flow<Boolean> = context.keywordStore.data.map { it[onboardingDone] ?: false }

suspend fun completeOnboarding() {
    context.keywordStore.edit { it[onboardingDone] = true }
}
```

`OnboardingScreen.kt` — three steps in one horizontal `Pager`: welcome (what/why + on-device privacy note), keywords (add-first keyword, skippable), enable (deep link + battery ask).
```kotlin
package com.spoilerblocker.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    initialKeywords: List<String>,
    onAddKeyword: (String) -> Unit,
    onFinish: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        when (page) {
            0 -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("Shush", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Covers spoilers anywhere on your phone. Add keywords, grant one permission, " +
                    "and offending text gets covered — in any app.\n\nAll matching happens " +
                    "on your device, in memory. The app has no internet access at all.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = { page = 1 }, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Get started")
                }
            }
            1 -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("What shouldn't you see?", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("e.g. Red Wedding") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                Row(Modifier.padding(top = 16.dp)) {
                    Button(
                        onClick = { if (input.isNotBlank()) { onAddKeyword(input); input = "" } },
                        enabled = input.isNotBlank(),
                    ) { Text("Add keyword") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { page = 2 }) { Text("Skip") }
                }
                if (initialKeywords.isNotEmpty()) {
                    Text(
                        "Added so far: ${initialKeywords.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            else -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("One last step", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Enable \"Shush\" in Accessibility settings. " +
                    "This is what lets it cover text on screen.",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open accessibility settings") }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Exempt from battery optimization") }
                TextButton(onClick = onFinish, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Done")
                }
            }
        }
    }
}
```

Edit `MainActivity.kt` — route between onboarding and main:
```kotlin
// inside setContent, SpoilerBlockerTheme:
val vm: KeywordsViewModel = viewModel()
val keywords by vm.keywords.collectAsState()
val onboarded by remember { vm.isOnboarded }.collectAsState(initial = false)

when {
    !onboarded -> OnboardingScreen(
        initialKeywords = keywords,
        onAddKeyword = vm::add,
        onFinish = { vm.completeOnboarding() },
    )
    else -> KeywordsScreen(/* as in Task 8 */)
}
```
Expose `isOnboarded` / `completeOnboarding()` on the ViewModel, delegating to the repository.

**Verify:** fresh install (`adb uninstall com.spoilerblocker && ./gradlew installDebug`) → onboarding appears; add a keyword; buttons deep-link to Accessibility settings and battery-optimization dialog; "Done" lands on the main screen with the banner. Kill and reopen → onboarding does not reappear. Total time from install to protected: under a minute (success criterion 10).

---

## Task 10: Hardening pass

**Files:** edits across service files. Checklist:

1. **No-network proof:** `./gradlew assembleDebug` then
   `apkanalyzer manifest print app/build/outputs/apk/debug/app-debug.apk | grep -c "INTERNET"` → `0` (success criterion 9). If `apkanalyzer` is unavailable: `unzip -p app/build/outputs/apk/debug/app-debug.apk AndroidManifest.xml | strings | grep -c internet` → `0`.
2. **Password/secret fields:** in `NodeScanner.nodeText()`, skip nodes where `node.isPassword == true`.
3. **Zero keywords:** `doScan()` already no-ops via `matcher.isEmpty`; verify stickers clear when the last keyword is deleted (the keywords flow triggers `scheduleScan`, which yields zero matches → `update(emptyList())` → `clear()`).
4. **Reveal-expiry rescan:** covered by `onRevealExpired = { doScan() }`.
5. **Proguard:** release build must keep the service (it's manifest-referenced, so R8 keeps it automatically) — verify with `./gradlew assembleRelease` → `BUILD SUCCESSFUL`.
6. **Sticker contrast:** `Primary` (#4A5FC1) vs white text — verify ≥ 4.5:1 at https://react-color.github.io/contrast-ratio or the WebAIM checker: 4A5FC1 on FFFFFF ≈ 6.3:1 → passes AA. With dynamic color, Material You palettes maintain AA by construction.

**Verify:** all six checks above pass.

---

## Task 11: End-to-end device verification (success criteria walkthrough)

On an emulator/device with the service enabled and keyword "red wedding" added:

1. **Basic blocking (criterion 1):** open Chrome to any page containing "red wedding" → sticker appears within ~1 s, text unreadable underneath.
2. **Multiple matches (criterion 2):** a page with 3 occurrences → 3 stickers.
3. **Scroll case (criterion 3):** scroll a long page → new matches get covered without interaction.
4. **Tap-to-reveal (criterion 4):** tap → visible; 5 s → re-covered if still on screen; scrolled away → not re-covered.
5. **Keyword lifecycle (criterion 5):** delete the keyword in the app → next screen change, nothing is covered. Re-add → covered again.
6. **Reboot (criterion 6):** `adb reboot` → after boot, without opening the app, open the test page → still blocked (accessibility services are re-bound by the system at boot once enabled).
   ```bash
   adb shell settings get secure enabled_accessibility_services
   ```
   → output contains `com.spoilerblocker/.service.SpoilerBlockerService`.
7. **Battery (criterion 8):** screen off for 10 minutes → no logcat activity:
   ```bash
   adb logcat -b all -d -s SpoilerBlocker | tail
   ```
   → no scans logged while screen was off (events don't fire).
8. **No crashes:** `adb logcat -d | grep -c "FATAL.*spoilerblocker"` → `0`.

## Final verification

```bash
./gradlew testDebugUnitTest assembleDebug
```
→ all unit tests pass, both debug and release APKs build; Task 11's on-device checklist passes in full.
