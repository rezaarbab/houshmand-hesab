package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.db.Debt
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.ConfirmDialog
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.JalaliDatePickerDialog
import com.houshmandhesab.app.ui.theme.ExpenseRed
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun toPersianCompat(s: String, persian: Boolean): String =
    if (persian) Format.toPersianDigits(s) else s

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {
    val debts = wallet.debts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(debt: Debt) = viewModelScope.launch { wallet.saveDebt(debt) }
    fun delete(id: Long) = viewModelScope.launch { wallet.deleteDebt(id) }
    fun settle(id: Long) = viewModelScope.launch { wallet.settleDebt(id) }
}

@Composable
fun DebtsScreen(
    onBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }
    var filter by remember { mutableStateOf("ALL") }

    val filtered = when (filter) {
        "OWE" -> debts.filter { !it.owedToMe && !it.settled }
        "MINE" -> debts.filter { it.owedToMe && !it.settled }
        "SETTLED" -> debts.filter { it.settled }
        else -> debts.filter { !it.settled }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_debt))
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
                    stringResource(R.string.debts_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text(stringResource(R.string.filter_all)) })
                FilterChip(selected = filter == "OWE", onClick = { filter = "OWE" }, label = { Text(stringResource(R.string.i_owe)) })
                FilterChip(selected = filter == "MINE", onClick = { filter = "MINE" }, label = { Text(stringResource(R.string.owed_to_me)) })
                FilterChip(selected = filter == "SETTLED", onClick = { filter = "SETTLED" }, label = { Text(stringResource(R.string.settled)) })
            }
            if (filtered.isEmpty()) {
                EmptyState(Icons.Rounded.People, stringResource(R.string.no_debts))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            persian = persian,
                            currency = currency,
                            onSettle = { viewModel.settle(debt.id) },
                            onDelete = { deletingId = debt.id }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        DebtDialog(onDismiss = { showAdd = false }, onSave = {
            viewModel.save(it)
            showAdd = false
        })
    }
    if (deletingId != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete),
            onConfirm = { viewModel.delete(deletingId!!); deletingId = null },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
private fun DebtCard(debt: Debt, persian: Boolean, currency: String, onSettle: () -> Unit, onDelete: () -> Unit) {
    val isMine = debt.owedToMe
    val tint = if (isMine) IncomeGreen else ExpenseRed
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    debt.person.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    color = tint,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(debt.person, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (isMine) R.string.owed_to_me else R.string.i_owe) +
                        if (debt.dueDate != null) " • " + Jalali.format(debt.dueDate!!, persian) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Format.money(debt.amount, persian, currency),
                style = MaterialTheme.typography.titleSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
        if (debt.note.isNotBlank()) {
            Text(
                debt.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (!debt.settled && debt.installmentsTotal > 0) {
            Text(
                if (persian) Format.toPersianDigits("${debt.installmentsPaid}/${debt.installmentsTotal}")
                else "${debt.installmentsPaid}/${debt.installmentsTotal}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (!debt.settled) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onSettle) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.mark_settled))
                }
            }
        } else {
            Text(
                "✓ " + stringResource(R.string.settled),
                style = MaterialTheme.typography.labelMedium,
                color = IncomeGreen,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DebtDialog(onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var owedToMe by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_debt)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !owedToMe, onClick = { owedToMe = false }, label = { Text(stringResource(R.string.i_owe)) })
                    FilterChip(selected = owedToMe, onClick = { owedToMe = true }, label = { Text(stringResource(R.string.owed_to_me)) })
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text(stringResource(R.string.person_name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (amount.isEmpty()) "" else Format.money(Format.parseAmount(amount), false),
                    onValueChange = { amount = Format.parseAmount(it).toString() },
                    label = { Text(stringResource(R.string.debt_amount)) },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note)) },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (dueDate == null) "+ " + stringResource(R.string.due_date)
                        else Jalali.format(dueDate!!, true)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = Format.parseAmount(amount)
                if (person.isNotBlank() && v > 0) {
                    onSave(
                        Debt(
                            person = person.trim(),
                            amount = v,
                            owedToMe = owedToMe,
                            dueDate = dueDate,
                            note = note.trim()
                        )
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = Jalali.today(),
            onDismiss = { showDatePicker = false },
            onSelected = { dueDate = Jalali.toMillis(it.jy, it.jm, it.jd); showDatePicker = false }
        )
    }
}
