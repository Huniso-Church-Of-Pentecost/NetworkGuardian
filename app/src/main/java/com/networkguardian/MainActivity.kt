package com.networkguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.networkguardian.security.AutoLockOption
import com.networkguardian.ui.navigation.GuardianNavHost
import com.networkguardian.ui.screens.LockScreen
import com.networkguardian.ui.theme.NetworkGuardianTheme
import com.networkguardian.ui.viewmodel.GuardianViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var graph: AppGraph
    private var lastBackgroundedEpochMs: Long = 0L

    private val viewModel: GuardianViewModel by viewModels {
        GuardianViewModel.factory(AppGraph.get(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph = AppGraph.get(this)

        setContent {
            NetworkGuardianTheme {
                var unlocked by remember { mutableStateOf(true) } // real gating wired below via LaunchedEffect
                var pinConfigured by remember { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val hasPin = graph.appLockManager.isPinSet()
                    pinConfigured = hasPin
                    unlocked = !hasPin // if no PIN set up yet, don't block the first run
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (unlocked) {
                        GuardianNavHost(viewModel)
                    } else {
                        LockScreen(
                            biometricAvailable = false,
                            onBiometricRequested = { /* Wired via BiometricAuthManager when running on a FragmentActivity host */ },
                            onUnlock = { pin ->
                                var success = false
                                kotlinx.coroutines.runBlocking {
                                    success = graph.appLockManager.verifyPin(pin)
                                }
                                if (success) unlocked = true
                                success
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundedEpochMs = System.currentTimeMillis()
    }
}
