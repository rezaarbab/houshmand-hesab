package com.houshmandhesab.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.houshmandhesab.app.ui.navigation.AppRoot
import com.houshmandhesab.app.ui.screens.LockScreen
import com.houshmandhesab.app.ui.theme.HesabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by androidx.activity.viewModels()

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
                    LockGate()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LockGate(mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
        val lockEnabled by mainViewModel.lockEnabled.collectAsStateWithLifecycle(initialValue = false)
        val unlocked by mainViewModel.unlocked.collectAsStateWithLifecycle(initialValue = false)
        val pinHash by mainViewModel.pinHash.collectAsStateWithLifecycle(initialValue = "")
        val biometric by mainViewModel.biometricEnabled.collectAsStateWithLifecycle(initialValue = true)
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            mainViewModel.relock()
        }
        if (lockEnabled && pinHash.isNotBlank() && !unlocked) {
            LockScreen(
                pinHash = pinHash,
                biometricEnabled = biometric,
                onUnlock = mainViewModel::unlock
            )
        }
    }
}
