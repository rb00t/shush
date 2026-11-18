package com.shush.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.keywordStore by preferencesDataStore(name = "keywords")

class KeywordsRepository(private val context: Context) {

    private val key = stringPreferencesKey("keyword_list")
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val blockingEnabledKey = booleanPreferencesKey("blocking_enabled")

    /** Cold flow of the current keyword list, order preserved. */
    val keywords: Flow<List<String>> =
        context.keywordStore.data.map { prefs ->
            prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        }

    /** Emits the stored value, defaulting to false when never set. */
    val isOnboarded: Flow<Boolean> =
        context.keywordStore.data.map { prefs -> prefs[onboardingDone] ?: false }

    suspend fun completeOnboarding() {
        context.keywordStore.edit { it[onboardingDone] = true }
    }

    /** Master on/off switch for spoiler blocking. Defaults to enabled. */
    val blockingEnabled: Flow<Boolean> =
        context.keywordStore.data.map { prefs -> prefs[blockingEnabledKey] ?: true }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.keywordStore.edit { it[blockingEnabledKey] = enabled }
    }

    suspend fun current(): List<String> = keywords.first()

    suspend fun add(rawKeyword: String) {
        val keyword = rawKeyword.trim()
        if (keyword.isEmpty() || keyword.length > MAX_KEYWORD_LENGTH) return
        context.keywordStore.edit { prefs ->
            val current = prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
            if (current.size >= MAX_KEYWORDS) return@edit
            // Dedupe case-insensitively, but keep the user's original casing for display.
            if (current.none { it.equals(keyword, ignoreCase = true) }) {
                prefs[key] = (current + keyword).joinToString(SEPARATOR)
            }
        }
    }

    suspend fun remove(keyword: String) {
        context.keywordStore.edit { prefs ->
            val current = prefs[key]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: return@edit
            prefs[key] = current
                .filterNot { it.equals(keyword.trim(), ignoreCase = true) }
                .joinToString(SEPARATOR)
        }
    }

    private companion object {
        const val SEPARATOR = "\u0000"
        const val MAX_KEYWORDS = 200
        const val MAX_KEYWORD_LENGTH = 64
    }
}
