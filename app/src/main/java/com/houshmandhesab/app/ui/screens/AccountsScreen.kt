package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.db.Account
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.COLOR_PALETTE
import com.houshmandhesab.app.ui.components.ConfirmDialog
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.MoneyText
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.toColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {
    val accounts = wallet.accounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val balances = wallet.balances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(account: Account) = viewModelScope.launch { wallet.saveAccount(account) }
    fun delete(id: Long) = viewModelScope.launch { wallet.deleteAccount(id) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val balances by viewModel.balances.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Account?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_account))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Text(
                    stringResource(R.string.accounts_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (accounts.isEmpty()) {
                EmptyState(Icons.Rounded.Wallet, stringResource(R.string.no_data))
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                                .clickable { editing = account }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .background(toColor(account.color).copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    accountTypeIcon(account.type),
                                    contentDescription = null,
                                    tint = toColor(account.color),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(account.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    accountTypeLabel(account.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MoneyText(
                                amount = balances[account.id] ?: 0L,
                                persianDigits = persian,
                                symbol = currency,
                                style = MaterialTheme.typography.titleSmall,
                                color = IncomeGreen
                            )
                            IconButton(onClick = { deletingId = account.id }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        AccountDialog(
            account = editing,
            onDismiss = { showAdd = false; editing = null },
            onSave = { viewModel.save(it); showAdd = false; editing = null }
        )
    }
    if (deletingId != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_account_confirm),
            onConfirm = { viewModel.delete(deletingId!!); deletingId = null },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
private fun accountTypeLabel(type: String): String = when (type) {
    "BANK" -> stringResource(R.string.account_bank)
    "WALLET" -> stringResource(R.string.account_wallet)
    else -> stringResource(R.string.account_cash)
}

private fun accountTypeIcon(type: String): ImageVector = when (type) {
    "BANK" -> Icons.Rounded.AccountBalance
    "WALLET" -> Icons.Rounded.Wallet
    else -> Icons.Rounded.Payments
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: "CASH") }
    var initial by remember { mutableStateOf(if ((account?.initialBalance ?: 0L) == 0L) "" else account!!.initialBalance.toString()) }
    var color by remember { mutableStateOf(account?.color ?: COLOR_PALETTE.first()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (account == null) R.string.add_account else R.string.edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.account_name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH", "BANK", "WALLET").forEach { t ->
                        val label = when (t) {
                            "BANK" -> stringResource(R.string.account_bank)
                            "WALLET" -> stringResource(R.string.account_wallet)
                            else -> stringResource(R.string.account_cash)
                        }
                        Box(
                            Modifier
                                .background(
                                    if (type == t) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { type = t }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = if (initial.isEmpty()) "" else Format.money(Format.parseAmount(initial), false),
                    onValueChange = { initial = Format.parseAmount(it).toString() },
                    label = { Text(stringResource(R.string.initial_balance)) },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLOR_PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(toColor(c), CircleShape)
                                .clickable { color = c }
                                .padding(3.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            Account(
                                id = account?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                color = color,
                                initialBalance = Format.parseAmount(initial)
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
