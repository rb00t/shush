package com.shush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shush.R
import kotlinx.coroutines.launch

@Composable
fun KeywordsScreen(
    serviceActive: Boolean,
    blockingEnabled: Boolean,
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onFixService: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    fun submit() {
        val candidate = input.trim()
        if (candidate.isEmpty()) return
        focusManager.clearFocus()
        if (keywords.any { it.equals(candidate, ignoreCase = true) }) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.already_added))
            }
            return
        }
        onAdd(candidate)
        input = ""
    }

    fun remove(keyword: String) {
        onRemove(keyword)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.keyword_deleted, keyword),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAdd(keyword)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            StatusBanner(serviceActive, blockingEnabled, keywords.size, onFix = onFixService)

            val toggleSupporting = when {
                !serviceActive -> stringResource(R.string.blocking_service_off)
                blockingEnabled -> stringResource(R.string.blocking_active)
                else -> stringResource(R.string.blocking_paused)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.blocking_title)) },
                supportingContent = { Text(toggleSupporting) },
                trailingContent = {
                    Switch(checked = blockingEnabled, onCheckedChange = null)
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .toggleable(
                        value = blockingEnabled,
                        role = Role.Switch,
                        onValueChange = onToggleBlocking,
                    ),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Text(
                text = stringResource(R.string.keywords_header, keywords.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )

            if (keywords.isEmpty()) {
                EmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(keywords, key = { _, k -> k }) { index, keyword ->
                        ListItem(
                            headlineContent = { Text(keyword) },
                            trailingContent = {
                                IconButton(onClick = { remove(keyword) }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_keyword, keyword),
                                    )
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        if (index < keywords.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 64) input = it },
                    placeholder = { Text(stringResource(R.string.add_keyword_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    trailingIcon = {
                        IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_keyword),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            RoundedCornerShape(28.dp),
                        ),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
