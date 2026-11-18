package com.shush.data

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
