package com.shush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shush.R

/** Three honest states: protected, paused via the master switch, service off. */
@Composable
fun StatusBanner(
    serviceActive: Boolean,
    blockingEnabled: Boolean,
    keywordCount: Int,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paused = serviceActive && !blockingEnabled
    val container = when {
        !serviceActive -> MaterialTheme.colorScheme.errorContainer
        paused -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = when {
        !serviceActive -> MaterialTheme.colorScheme.onErrorContainer
        paused -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val icon = when {
        !serviceActive -> Icons.Outlined.VisibilityOff
        paused -> Icons.Outlined.Pause
        else -> Icons.Outlined.Shield
    }
    val title = when {
        !serviceActive -> stringResource(R.string.banner_off)
        paused -> stringResource(R.string.banner_paused)
        else -> stringResource(R.string.banner_protected)
    }
    val supporting = when {
        !serviceActive -> stringResource(R.string.banner_off_supporting)
        paused -> null   // the toggle row below already states this
        else -> pluralStringResource(R.plurals.banner_supporting_count, keywordCount, keywordCount)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = onContainer,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 44.dp, top = 4.dp)
            )
        }
        if (!serviceActive) {
            FilledTonalButton(
                onClick = onFix,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = onContainer,
                    contentColor = container,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 16.dp)
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}
