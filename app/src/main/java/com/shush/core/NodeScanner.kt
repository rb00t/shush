package com.shush.core

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A screen rectangle containing spoiler text. Node text itself is never retained. */
data class KeywordMatch(val bounds: Rect)

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
            if (!node.isVisibleToUser) continue   // never cover off-screen/invisible nodes
            val text = nodeText(node)
            if (text != null && matcher.matches(text)) {
                val rect = Rect().also { node.getBoundsInScreen(it) }
                if (rect.width() > 0 && rect.height() > 0) {
                    matches += KeywordMatch(rect)
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return matches
    }

    private fun nodeText(node: AccessibilityNodeInfo): String? {
        if (node.isPassword) return null   // never match password fields
        // WebView container nodes carry the page title as `text` with screen-wide
        // bounds; matching them would cover the entire viewport. Real content is
        // exposed as fine-grained child nodes, so skip the container.
        if (node.className?.toString() == "android.webkit.WebView") return null
        val t = node.text?.toString()?.takeIf { it.isNotBlank() }
        if (t != null) return t
        val cd = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        return cd
    }
}
