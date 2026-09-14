package com.houshmandhesab.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.data.repo.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {

    val themeMode = wallet.settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")

    val lockEnabled = wallet.settings.lockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val biometricEnabled = wallet.settings.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val pinHash = wallet.settings.pinHash
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()

    fun unlock() {
        _unlocked.value = true
    }

    fun relock() {
        if (pinHash.value.isNotBlank()) _unlocked.value = false
    }

    init {
        viewModelScope.launch {
            wallet.seedIfFirstRun()
        }
    }
}
