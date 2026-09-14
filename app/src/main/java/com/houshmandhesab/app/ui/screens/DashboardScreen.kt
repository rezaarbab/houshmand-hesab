package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.houshmandhesab.app.R
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.GradientCard
import com.houshmandhesab.app.ui.components.GlassCard
import com.houshmandhesab.app.ui.components.MiniBars
import com.houshmandhesab.app.ui.components.MoneyText
import com.houshmandhesab.app.ui.components.SectionHeader
import com.houshmandhesab.app.ui.components.EmptyState
import com.houshmandhesab.app.ui.components.TxRow
import com.houshmandhesab.app.ui.navigation.Routes
import com.houshmandhesab.app.ui.theme.ExpenseRed
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.ui.theme.TransferBlue
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {

    private val today = Jalali.today()
    private val monthFrom = Jalali.startOfMonthMillis(today.jy, today.jm)
    private val monthTo = Jalali.endOfMonthMillis(today.jy, today.jm)
    private val weekFrom = System.currentTimeMillis() - 7L * 24 * 3600 * 1000

    val totalBalance = wallet.totalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthTotals = wallet.monthTotals(monthFrom, monthTo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val daily = wallet.dailyTotals(weekFrom, System.currentTimeMillis() + 1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recent = wallet.recentTransactions(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets = wallet.budgetProgress(monthFrom, monthTo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
}

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val balance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totals by viewModel.monthTotals.collectAsStateWithLifecycle()
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title)
                    )
                }
            }
        }
        item {
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.total_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    Format.money(balance, persian, currency.ifBlank { "" }),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MiniStat(
                        icon = Icons.Rounded.TrendingUp,
                        label = stringResource(R.string.this_month_income),
                        amount = totals?.income ?: 0L,
                        persian = persian,
                        symbol = currency
                    )
                    MiniStat(
                        icon = Icons.Rounded.TrendingDown,
                        label = stringResource(R.string.this_month_expense),
                        amount = totals?.expense ?: 0L,
                        persian = persian,
                        symbol = currency
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(
                    Icons.Rounded.TrendingDown, stringResource(R.string.quick_add_expense),
                    ExpenseRed
                ) { onNavigate(Routes.add("EXPENSE")) }
                QuickAction(
                    Icons.Rounded.TrendingUp, stringResource(R.string.quick_add_income),
                    IncomeGreen
                ) { onNavigate(Routes.add("INCOME")) }
                QuickAction(
                    Icons.Rounded.SwapHoriz, stringResource(R.string.quick_transfer),
                    TransferBlue
                ) { onNavigate(Routes.add("TRANSFER")) }
                QuickAction(
                    Icons.Rounded.AutoAwesome, stringResource(R.string.quick_ai),
                    Color(0xFFA78BFA)
                ) { onNavigate(Routes.AI) }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.spending_last_7_days),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                MiniBars(
                    values = daily.map { it.expense.toFloat() },
                    color = ExpenseRed
                )
            }
        }
        if (budgets.isNotEmpty()) {
            item {
                SectionHeader(
                    stringResource(R.string.monthly_budgets),
                    actionText = stringResource(R.string.view_all)
                ) { onNavigate(Routes.BUDGETS) }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    budgets.take(2).forEach { b ->
                        val fraction =
                            if (b.limitAmount > 0) (b.spent.toFloat() / b.limitAmount).coerceIn(0f, 1f) else 0f
                        GlassCard(Modifier.weight(1f)) {
                            Text(
                                b.name ?: stringResource(R.string.unknown_category),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = if (fraction >= 1f) ExpenseRed else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                Format.compact(b.spent, persian) + " / " + Format.compact(b.limitAmount, persian),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (budgets.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item {
            SectionHeader(
                stringResource(R.string.recent_transactions),
                actionText = stringResource(R.string.view_all)
            ) { onNavigate(Routes.TRANSACTIONS) }
        }
        if (recent.isEmpty()) {
            item { EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.no_transactions)) }
        } else {
            items(recent, key = { it.tx.id }) { tx ->
                TxRow(tx, persian) { onNavigate(Routes.add(id = tx.tx.id)) }
            }
        }
        item { SectionHeader(stringResource(R.string.my_accounts)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    Icons.Rounded.AccountBalanceWallet,
                    stringResource(R.string.my_accounts),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.ACCOUNTS) }
                SectionCard(
                    Icons.Rounded.Category,
                    stringResource(R.string.manage_categories),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.CATEGORIES) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    Icons.Rounded.Savings,
                    stringResource(R.string.monthly_budgets),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.BUDGETS) }
                SectionCard(
                    Icons.Rounded.Flag,
                    stringResource(R.string.savings_goals),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.GOALS) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    Icons.Rounded.People,
                    stringResource(R.string.debts_loans),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.DEBTS) }
                SectionCard(
                    Icons.Rounded.PieChart,
                    stringResource(R.string.full_report),
                    Modifier.weight(1f)
                ) { onNavigate(Routes.REPORTS) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, amount: Long, persian: Boolean, symbol: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
        Column(Modifier.padding(start = 6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                Format.money(amount, persian, symbol),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(tint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SectionCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ),
                androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 10.dp),
            maxLines = 1
        )
    }
}
