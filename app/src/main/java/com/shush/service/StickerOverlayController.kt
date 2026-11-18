package com.shush.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.shush.R
import com.shush.core.KeywordMatch

/**
 * Full-screen spoiler cover.
 *
 * When any keyword match is visible on screen, an opaque cover blocks the
 * entire screen. The cover window is FLAG_NOT_TOUCHABLE, so touches pass
 * straight through: the user can scroll the spoiler content out of view,
 * and the cover lifts as soon as a rescan finds no matches.
 *
 * A small tappable pill ("Reveal · 5s") rides on top of the cover so the
 * user can still peek: tapping it uncovers everything for [REVEAL_MS],
 * after which a rescan re-covers if spoilers are still on screen.
 */
class StickerOverlayController(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density

    private var cover: View? = null
    private var pill: View? = null
    private var revealedUntil = 0L
    private var revealRunnable: Runnable? = null

    /** Called after every scan. */
    fun update(matches: List<KeywordMatch>) {
        if (matches.isEmpty()) {
            clear()
            return
        }
        // Within the reveal window the screen stays uncovered;
        // the expiry rescan re-covers if spoilers remain.
        if (SystemClock.elapsedRealtime() < revealedUntil) return
        if (cover?.isAttachedToWindow != true) {
            removeCover()   // stale/dead window reference: rebuild
            show()
        }
    }

    fun clear() {
        removeCover()
        revealedUntil = 0
    }

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun show() {
        val wm = this.wm ?: return
        val dm = context.resources.displayMetrics
        val sb = statusBarHeight()

        // The system insets accessibility-overlay content below the status bar;
        // shift up by that inset and extend the height so the cover is truly full-screen.
        val coverParams = WindowManager.LayoutParams(
            dm.widthPixels,
            dm.heightPixels + sb,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or      // touches pass through: user can scroll
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = -sb
        }

        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (TOP_OFFSET_DP * density).toInt()
        }

        runCatching {
            val coverView = View.inflate(context, R.layout.cover, null)
            coverView.contentDescription = context.getString(R.string.cover_accessibility)
            coverView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            wm.addView(coverView, coverParams)
            cover = coverView
            coverView.announceForAccessibility(context.getString(R.string.cover_accessibility))
        }
        runCatching {
            val pillView = View.inflate(context, R.layout.pill, null)
            pillView.contentDescription = context.getString(R.string.reveal_pill_accessibility)
            pillView.setOnClickListener { reveal() }
            wm.addView(pillView, pillParams)
            pill = pillView
        }
        if (cover == null) removeCover()   // cover failed to add: don't strand the pill
    }

    /** Tap the pill: uncover everything for [REVEAL_MS]; expiry rescan re-covers if needed. */
    private fun reveal() {
        revealedUntil = SystemClock.elapsedRealtime() + REVEAL_MS
        removeCover()
        revealRunnable?.let(handler::removeCallbacks)
        val r = Runnable {
            revealRunnable = null
            onRevealExpired?.invoke()   // service triggers doScan()
        }
        revealRunnable = r
        handler.postDelayed(r, REVEAL_MS)
    }

    private fun removeCover() {
        cover?.let { runCatching { wm.removeView(it) } }
        pill?.let { runCatching { wm.removeView(it) } }
        cover = null
        pill = null
        revealRunnable?.let(handler::removeCallbacks)
        revealRunnable = null
    }

    var onRevealExpired: (() -> Unit)? = null

    private companion object {
        const val REVEAL_MS = 5_000L
        const val TOP_OFFSET_DP = 80
    }
}
