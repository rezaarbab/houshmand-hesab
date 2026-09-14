package com.houshmandhesab.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.prefs.SettingsRepository
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.util.Format
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val wallet: WalletRepository,
    val settings: SettingsRepository
) : ViewModel() {

    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val persianDigits = settings.persianDigits.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = settings.currencySymbol.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val cfAccount = settings.cfAccountId.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val cfToken = settings.cfToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val cfModel = settings.cfModel.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_MODEL)
    val lockEnabled = settings.lockEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val biometricEnabled = settings.biometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notificationsEnabled = settings.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val pinHash = settings.pinHash.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setTheme(v: String) = viewModelScope.launch { settings.setThemeMode(v) }
    fun setPersianDigits(v: Boolean) = viewModelScope.launch { settings.setPersianDigits(v) }
    fun setCurrency(v: String) = viewModelScope.launch { settings.setCurrencySymbol(v) }
    fun setCfAccount(v: String) = viewModelScope.launch { settings.setCfAccountId(v) }
    fun setCfToken(v: String) = viewModelScope.launch { settings.setCfToken(v) }
    fun setCfModel(v: String) = viewModelScope.launch { settings.setCfModel(v) }
    fun setLockEnabled(v: Boolean) = viewModelScope.launch { settings.setLockEnabled(v) }
    fun setBiometric(v: Boolean) = viewModelScope.launch { settings.setBiometricEnabled(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { settings.setNotificationsEnabled(v) }
    fun setPin(pin: String) = viewModelScope.launch { settings.setPin(pin) }
    fun disablePin() = viewModelScope.launch { settings.setPinHash("") }

    fun exportBackup(context: android.content.Context, onDone: (String) -> Unit) = viewModelScope.launch {
        val json = wallet.exportJson()
        withContext(Dispatchers.IO) {
            val file = File(wallet.exportsDir(), "hesab_backup_${System.currentTimeMillis()}.json")
            file.writeText(json)
            onDone(file.absolutePath)
        }
    }

    fun exportCsv(context: android.content.Context, onDone: (String) -> Unit) = viewModelScope.launch {
        val csv = wallet.exportCsv()
        withContext(Dispatchers.IO) {
            val file = File(wallet.exportsDir(), "hesab_export_${System.currentTimeMillis()}.csv")
            file.writeText(csv)
            onDone(file.absolutePath)
        }
    }

    fun restore(text: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(wallet.importJson(text))
    }

    fun clearAll(onDone: () -> Unit) = viewModelScope.launch {
        wallet.clearAllData()
        onDone()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val persianDigits by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val cfAccount by viewModel.cfAccount.collectAsStateWithLifecycle()
    val cfToken by viewModel.cfToken.collectAsStateWithLifecycle()
    val cfModel by viewModel.cfModel.collectAsStateWithLifecycle()
    val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle()
    val biometric by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val pinSet by viewModel.pinHash.collectAsStateWithLifecycle()

    var currencyDraft by remember(currency) { mutableStateOf(currency) }
    var accountDraft by remember(cfAccount) { mutableStateOf(cfAccount) }
    var tokenDraft by remember(cfToken) { mutableStateOf(cfToken) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }
                if (text != null) {
                    viewModel.restore(text) { ok ->
                        Toast.makeText(
                            context,
                            if (ok) context.getString(R.string.restore_done)
                            else context.getString(R.string.ai_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))

        SectionTitle(stringResource(R.string.appearance))
        Text(stringResource(R.string.theme_system), style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            SegmentedButton(
                selected = theme == "SYSTEM",
                onClick = { viewModel.setTheme("SYSTEM") },
                shape = SegmentedButtonDefaults.itemShape(0, 3)
            ) { Text(stringResource(R.string.theme_system)) }
            SegmentedButton(
                selected = theme == "LIGHT",
                onClick = { viewModel.setTheme("LIGHT") },
                shape = SegmentedButtonDefaults.itemShape(1, 3)
            ) { Text(stringResource(R.string.theme_light)) }
            SegmentedButton(
                selected = theme == "DARK",
                onClick = { viewModel.setTheme("DARK") },
                shape = SegmentedButtonDefaults.itemShape(2, 3)
            ) { Text(stringResource(R.string.theme_dark)) }
        }
        ToggleRow(stringResource(R.string.persian_digits), persianDigits) { viewModel.setPersianDigits(it) }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.currency_symbol), modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = currencyDraft,
                onValueChange = { currencyDraft = it },
                singleLine = true,
                modifier = Modifier.width(120.dp),
                placeholder = { Text("تومان") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { viewModel.setCurrency(currencyDraft) }) { Text(stringResource(R.string.save)) }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SectionTitle(stringResource(R.string.ai_settings))
        Text(
            stringResource(R.string.about_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = accountDraft,
            onValueChange = { accountDraft = it },
            label = { Text(stringResource(R.string.cloudflare_account_id)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = tokenDraft,
            onValueChange = { tokenDraft = it },
            label = { Text(stringResource(R.string.cloudflare_api_token)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ai_model), style = MaterialTheme.typography.labelMedium)
        SettingsRepository.AVAILABLE_MODELS.forEach { m ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = cfModel == m,
                    onClick = { viewModel.setCfModel(m) }
                )
                Text(m, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = {
            viewModel.setCfAccount(accountDraft)
            viewModel.setCfToken(tokenDraft)
        }) {
            Text(stringResource(R.string.save))
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SectionTitle(stringResource(R.string.security))
        ToggleRow(stringResource(R.string.enable_lock), lockEnabled) { enabled ->
            if (enabled) showPinDialog = true else viewModel.disablePin()
        }
        if (lockEnabled && pinSet.isNotBlank()) {
            ToggleRow(stringResource(R.string.biometric_unlock), biometric) { viewModel.setBiometric(it) }
            TextButton(onClick = { showPinDialog = true }) {
                Text(stringResource(R.string.change_pin))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SectionTitle(stringResource(R.string.notifications))
        ToggleRow(stringResource(R.string.notifications_enabled), notifications) { viewModel.setNotifications(it) }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SectionTitle(stringResource(R.string.data))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                viewModel.exportCsv(context) { path ->
                    shareFile(context, path, "text/csv")
                }
            }) { Text(stringResource(R.string.export_csv)) }
            OutlinedButton(onClick = {
                viewModel.exportBackup(context) { path ->
                    shareFile(context, path, "application/json")
                }
            }) { Text(stringResource(R.string.backup_json)) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { restorePicker.launch("*/*") }) {
            Text(stringResource(R.string.restore_json))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showClearConfirm = true }) {
            Text(stringResource(R.string.clear_all_data), color = MaterialTheme.colorScheme.error)
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SectionTitle(stringResource(R.string.about))
        Text(
            stringResource(R.string.about_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll {}
                    showClearConfirm = false
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onSet = { pin ->
                viewModel.setPin(pin)
                viewModel.setLockEnabled(true)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_pin)) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirm = it },
                    label = { Text(stringResource(R.string.confirm_pin)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length >= 4 && pin == confirm) onSet(pin)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun shareFile(context: android.content.Context, path: String, mime: String) {
    val file = File(path)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, file.name))
}
