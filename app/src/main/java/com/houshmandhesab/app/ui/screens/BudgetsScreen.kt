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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Savings
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
import com.houshmandhesab.app.data.db.Budget
import com.houshmandhesab.app.data.db.BudgetProgress
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.CategoryIcon
import com.houshmandhesab.app.ui.components.ConfirmDialog
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.theme.AccentAmber
import com.houshmandhesab.app.ui.theme.ExpenseRed
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import com.houshmandhesab.app.util.toColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {
    private val today = Jalali.today()
    private val from = Jalali.startOfMonthMillis(today.jy, today.jm)
    private val to = Jalali.endOfMonthMillis(today.jy, today.jm)

    val progress = wallet.budgetProgress(from, to)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenseCategories = wallet.categories("EXPENSE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(budget: Budget) = viewModelScope.launch { wallet.saveBudget(budget) }
    fun delete(id: Long) = viewModelScope.launch { wallet.deleteBudget(id) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_budget))
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
                    stringResource(R.string.budgets_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (progress.isEmpty()) {
                EmptyState(Icons.Rounded.Savings, stringResource(R.string.no_budgets))
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(progress, key = { it.budgetId }) { b ->
                        BudgetCard(
                            item = b,
                            persian = persian,
                            currency = currency,
                            onDelete = { deletingId = b.budgetId }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        BudgetDialog(
            categories = viewModel.expenseCategories.value,
            onDismiss = { showAdd = false },
            onSave = { viewModel.save(it); showAdd = false }
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
private fun BudgetCard(item: BudgetProgress, persian: Boolean, currency: String, onDelete: () -> Unit) {
    val fraction = if (item.limitAmount > 0) (item.spent.toFloat() / item.limitAmount).coerceIn(0f, 1f) else 0f
    val barColor = when {
        fraction >= 1f -> ExpenseRed
        fraction >= 0.7f -> AccentAmber
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(icon = item.icon ?: "more_horiz", color = item.color ?: 0xFF94A3B8, size = 38.dp, iconSize = 19.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(item.name ?: stringResource(R.string.unknown_category), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (item.spent >= item.limitAmount) stringResource(R.string.budget_over)
                    else stringResource(R.string.budget_remaining) + ": " + Format.money(
                        (item.limitAmount - item.spent).coerceAtLeast(0), persian, currency
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.spent >= item.limitAmount) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.budget_spent) + ": " + Format.money(item.spent, persian, currency),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                stringResource(R.string.budget_limit) + ": " + Format.money(item.limitAmount, persian, currency),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetDialog(
    categories: List<com.houshmandhesab.app.data.db.Category>,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var limit by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_budget)) },
        text = {
            Column {
                if (categories.isEmpty()) {
                    Text(stringResource(R.string.no_categories_hint), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(stringResource(R.string.select_category), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            Row(
                                Modifier
                                    .background(
                                        if (categoryId == cat.id) toColor(cat.color).copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { categoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(icon = cat.icon, color = cat.color, size = 22.dp, iconSize = 13.dp)
                                Text(cat.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = if (limit.isEmpty()) "" else Format.money(Format.parseAmount(limit), false),
                        onValueChange = { limit = Format.parseAmount(it).toString() },
                        label = { Text(stringResource(R.string.budget_limit)) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = Format.parseAmount(limit)
                    if (categoryId != null && amount > 0) {
                        onSave(Budget(categoryId = categoryId!!, limitAmount = amount))
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
