package com.shush.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.shush.R
import com.shush.service.isServiceEnabled

@Composable
fun OnboardingScreen(
    initialKeywords: List<String>,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    // Live service state: flips to "enabled" when the user returns from settings.
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun submitKeyword() {
        val candidate = input.trim()
        if (candidate.isEmpty()) return
        if (initialKeywords.none { it.equals(candidate, ignoreCase = true) }) {
            onAddKeyword(candidate)
        }
        input = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) {
                IconButton(onClick = { page-- }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))   // matches the back-button height on later steps
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.step_label, page + 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        when (page) {
            0 -> WelcomeStep(onContinue = { page = 1 }, Modifier.weight(1f))
            1 -> KeywordsStep(
                modifier = Modifier.weight(1f),
                input = input,
                onInputChange = { if (it.length <= 64) input = it },
                onSubmit = ::submitKeyword,
                keywords = initialKeywords,
                onRemoveKeyword = onRemoveKeyword,
                onContinue = { page = 2 },
                onSkip = { page = 2 },
            )
            else -> EnableStep(
                modifier = Modifier.weight(1f),
                serviceEnabled = serviceEnabled,
                onOpenSettings = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                onBatteryExempt = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                onFinish = onFinish,
            )
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.get_started))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordsStep(
    modifier: Modifier,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    keywords: List<String>,
    onRemoveKeyword: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            stringResource(R.string.onboarding_keywords_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
        )
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text(stringResource(R.string.keyword_example)) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            trailingIcon = {
                IconButton(onClick = onSubmit, enabled = input.isNotBlank()) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_keyword),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (keywords.isNotEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    keywords.forEach { keyword ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    keyword,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                                IconButton(onClick = { onRemoveKeyword(keyword) }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.remove_keyword, keyword),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Button(
            onClick = onContinue,
            enabled = keywords.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.continue_label))
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.skip_for_now))
        }
    }
}

@Composable
private fun EnableStep(
    modifier: Modifier,
    serviceEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onBatteryExempt: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.onboarding_enable_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            Text(
                stringResource(R.string.onboarding_enable_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (serviceEnabled) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.service_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.service_not_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.open_accessibility_settings))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBatteryExempt,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.exempt_battery))
        }
        Text(
            stringResource(R.string.battery_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (serviceEnabled) {
            FilledTonalButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.done))
            }
        } else {
            TextButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.continue_without_blocking))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
