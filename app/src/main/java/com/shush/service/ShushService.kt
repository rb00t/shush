package com.shush.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.shush.core.KeywordMatcher
import com.shush.core.NodeScanner
import com.shush.data.KeywordsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ShushService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingScan: Runnable? = null
    private var lastScanAt = 0L

    @Volatile private var matcher: KeywordMatcher = KeywordMatcher(emptyList())
    @Volatile private var blockingEnabled = true
    private lateinit var repository: KeywordsRepository

    private val overlayController by lazy {
        StickerOverlayController(this).also { it.onRevealExpired = { doScan() } }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (::repository.isInitialized) return   // guard against duplicate re-binds
        repository = KeywordsRepository(applicationContext)
        scope.launch {
            repository.keywords
                .catch { Log.w(TAG, "keyword flow failed", it) }
                .collect { list ->
                    matcher = KeywordMatcher(list)
                    scheduleScan()   // keyword edits/deletions take effect on next scan
                }
        }
        scope.launch {
            repository.blockingEnabled
                .catch { Log.w(TAG, "blocking flag flow failed", it) }
                .collect { enabled ->
                    blockingEnabled = enabled
                    if (enabled) scheduleScan() else overlayController.clear()
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
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            overlayController.clear()                        // stale stickers die with the window
        }
        scheduleScan()
    }

    private fun scheduleScan() {
        // Immediate-first, then throttled: the first event after a quiet period
        // scans right away (minimal visible-until-covered latency); bursts of
        // events are coalesced to at most one scan per MIN_SCAN_INTERVAL_MS.
        val now = SystemClock.elapsedRealtime()
        val wait = (MIN_SCAN_INTERVAL_MS - (now - lastScanAt)).coerceAtLeast(0)
        pendingScan?.let(handler::removeCallbacks)
        val r = Runnable { doScan() }
        pendingScan = r
        handler.postDelayed(r, wait)
    }

    private fun doScan() {
        pendingScan = null
        lastScanAt = SystemClock.elapsedRealtime()
        if (!blockingEnabled) {
            overlayController.clear()   // master switch off: never cover
            return
        }
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() == packageName) return   // never cover our own UI
        val matches = NodeScanner(matcher).scan(root)
        overlayController.update(matches)
        if (matches.isNotEmpty()) {
            Log.d(TAG, "covered ${matches.size} match(es)")
        }
    }

    override fun onInterrupt() { /* nothing to interrupt */ }

    override fun onDestroy() {
        pendingScan?.let(handler::removeCallbacks)
        overlayController.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Shush"
        private const val MIN_SCAN_INTERVAL_MS = 150L
    }
}
