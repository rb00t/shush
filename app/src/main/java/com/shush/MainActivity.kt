package com.shush

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shush.service.isServiceEnabled
import com.shush.ui.KeywordsScreen
import com.shush.ui.KeywordsViewModel
import com.shush.ui.onboarding.OnboardingScreen
import com.shush.ui.theme.ShushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShushTheme {
                val vm: KeywordsViewModel = viewModel()
                val keywords by vm.keywords.collectAsState()
                val onboarded by vm.isOnboarded.collectAsState()
                val blockingEnabled by vm.blockingEnabled.collectAsState()

                // Re-check the service state on every resume, so returning from
                // system settings flips the banner honestly.
                var serviceActive by remember { mutableStateOf(isServiceEnabled(this)) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            serviceActive = isServiceEnabled(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                when {
                    onboarded == null -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    !onboarded!! -> OnboardingScreen(
                        initialKeywords = keywords,
                        onAddKeyword = vm::add,
                        onRemoveKeyword = vm::remove,
                        onFinish = vm::completeOnboarding,
                    )
                    else -> KeywordsScreen(
                        serviceActive = serviceActive,
                        blockingEnabled = blockingEnabled,
                        keywords = keywords,
                        onAdd = vm::add,
                        onRemove = vm::remove,
                        onToggleBlocking = vm::setBlockingEnabled,
                        onFixService = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}
