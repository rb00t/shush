package com.shush.core

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

    @Test
    fun `unicode keyword matches across boundaries`() {
        val m = KeywordMatcher(listOf("Shippūden", "café"))
        assertTrue(m.matches("the Shippūden arc was great"))
        assertTrue(m.matches("un café !"))
        assertTrue(m.matches("loved CAFÉ AU LAIT"))      // UNICODE_CASE: É matches é
    }

    @Test
    fun `unicode keyword respects word boundaries`() {
        val m = KeywordMatcher(listOf("café"))
        assertFalse(m.matches("cafeteria"))             // followed by a letter
        assertFalse(m.matches("xcafé"))               // preceded by a letter
        assertTrue(m.matches("un café !"))             // punctuation boundary still matches
    }
}
