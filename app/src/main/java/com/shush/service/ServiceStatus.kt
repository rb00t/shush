package com.shush.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

fun isServiceEnabled(context: Context): Boolean {
    val accessibilityOn = Settings.Secure.getInt(
        context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0
    ) == 1
    if (!accessibilityOn) return false
    val expected = ComponentName(context, ShushService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any {
        it.equals(expected.flattenToString(), ignoreCase = true) ||
            it.equals(expected.flattenToShortString(), ignoreCase = true)
    }
}
