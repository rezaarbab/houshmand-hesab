package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.houshmandhesab.app.data.db.SavingsGoal
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.ConfirmDialog
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.JalaliDatePickerDialog
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {
    val goals = wallet.goals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(goal: SavingsGoal) = viewModelScope.launch { wallet.saveGoal(goal) }
    fun delete(id: Long) = viewModelScope.launch { wallet.deleteGoal(id) }
    fun addFunds(id: Long, amount: Long) = viewModelScope.launch { wallet.addToGoal(id, amount) }
}

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var fundingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_goal))
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
                    stringResource(R.string.goals_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (goals.isEmpty()) {
                EmptyState(Icons.Rounded.Flag, stringResource(R.string.no_goals))
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            persian = persian,
                            currency = currency,
                            onAddFunds = { fundingGoal = goal },
                            onDelete = { deletingId = goal.id }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        GoalDialog(onDismiss = { showAdd = false }, onSave = {
            viewModel.save(it)
            showAdd = false
        })
    }
    if (fundingGoal != null) {
        AddFundsDialog(
            goalTitle = fundingGoal!!.title,
            onDismiss = { fundingGoal = null },
            onAdd = { amount ->
                viewModel.addFunds(fundingGoal!!.id, amount)
                fundingGoal = null
            }
        )
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
private fun GoalCard(goal: SavingsGoal, persian: Boolean, currency: String, onAddFunds: () -> Unit, onDelete: () -> Unit) {
    val fraction = if (goal.targetAmount > 0) (goal.savedAmount.toFloat() / goal.targetAmount).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = IncomeGreen,
                modifier = Modifier.size(24.dp)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(goal.title, style = MaterialTheme.typography.titleSmall)
                if (goal.deadline != null) {
                    Text(
                        stringResource(R.string.deadline) + ": " + Jalali.format(goal.deadline!!, persian),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = IncomeGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    Format.money(goal.savedAmount, persian, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = IncomeGreen
                )
                Text(
                    Format.money(goal.targetAmount, persian, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (fraction >= 1f) {
                Text(
                    stringResource(R.string.goal_reached),
                    style = MaterialTheme.typography.labelLarge,
                    color = IncomeGreen
                )
            } else {
                TextButton(onClick = onAddFunds) { Text(stringResource(R.string.add_money)) }
            }
        }
    }
}

@Composable
private fun GoalDialog(onDismiss: () -> Unit, onSave: (SavingsGoal) -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_goal)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.goal_title)) },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = if (target.isEmpty()) "" else Format.money(Format.parseAmount(target), false),
                    onValueChange = { target = Format.parseAmount(it).toString() },
                    label = { Text(stringResource(R.string.target_amount)) },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (deadline == null) "+ " + stringResource(R.string.deadline)
                        else Jalali.format(deadline!!, true)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Format.parseAmount(target)
                if (title.isNotBlank() && amount > 0) {
                    onSave(SavingsGoal(title = title.trim(), targetAmount = amount, deadline = deadline))
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
            onSelected = { deadline = Jalali.toMillis(it.jy, it.jm, it.jd); showDatePicker = false }
        )
    }
}

@Composable
private fun AddFundsDialog(goalTitle: String, onDismiss: () -> Unit, onAdd: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_money) + " — " + goalTitle) },
        text = {
            OutlinedTextField(
                value = if (amount.isEmpty()) "" else Format.money(Format.parseAmount(amount), false),
                onValueChange = { amount = Format.parseAmount(it).toString() },
                label = { Text(stringResource(R.string.amount)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val v = Format.parseAmount(amount)
                if (v > 0) onAdd(v)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
