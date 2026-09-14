package com.houshmandhesab.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.houshmandhesab.app.ui.navigation.AppRoot
import com.houshmandhesab.app.ui.screens.LockScreen
import com.houshmandhesab.app.ui.theme.HesabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            HesabTheme(themeMode = theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                    LockGate(
                        viewModel = viewModel,
                        showLock = viewModel.lockEnabled.collectAsStateWithLifecycle(initialValue = false).value &&
                            viewModel.pinHash.collectAsStateWithLifecycle(initialValue = "").value.isNotBlank() &&
                            !viewModel.unlocked.collectAsStateWithLifecycle(initialValue = false).value,
                        biometric = viewModel.biometricEnabled.collectAsStateWithLifecycle(initialValue = true).value,
                        pinHash = viewModel.pinHash.collectAsStateWithLifecycle(initialValue = "").value,
                        onUnlock = viewModel::unlock
                    )
                }
            }
        }
    }
}

@Composable
private fun LockGate(
    viewModel: MainViewModel,
    showLock: Boolean,
    biometric: Boolean,
    pinHash: String,
    onUnlock: () -> Unit
) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.relock()
    }
    if (showLock) {
        LockScreen(
            pinHash = pinHash,
            biometricEnabled = biometric,
            onUnlock = onUnlock
        )
    }
}
