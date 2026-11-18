package com.shush.core

class KeywordMatcher(keywords: List<String>) {

    // Unicode property lookarounds instead of \b: \b is ASCII-only in Java regex,
    // which inverts match semantics for keywords with accented letters ("café").
    // \p{L}\p{N} are always Unicode in java.util.regex.
    // Case-insensitivity is achieved by Unicode-aware String.lowercase() on BOTH
    // keyword and text (Kotlin's lowercase() handles É→é, Ū→ū, etc.), so no
    // regex case flags are needed.
    private val patterns: List<Regex> = keywords
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .map { keyword ->
            Regex("(?<![\\p{L}\\p{N}_])${Regex.escape(keyword)}(?![\\p{L}\\p{N}_])")
        }

    val isEmpty: Boolean get() = patterns.isEmpty()

    /** True if [text] contains any keyword as a whole word, case-insensitive. */
    fun matches(text: String): Boolean {
        if (text.isEmpty() || patterns.isEmpty()) return false
        val lower = text.lowercase()   // once per text, not once per keyword
        return patterns.any { it.containsMatchIn(lower) }
    }
}
