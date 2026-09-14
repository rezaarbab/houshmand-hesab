package com.houshmandhesab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.houshmandhesab.app.data.repo.WalletRepository
import com.houshmandhesab.app.ui.components.BarGroup
import com.houshmandhesab.app.ui.components.ChartLegend
import com.houshmandhesab.app.ui.components.DonutChart
import com.houshmandhesab.app.ui.components.GlassCard
import com.houshmandhesab.app.ui.components.GroupedBarChart
import com.houshmandhesab.app.ui.components.PieLegend
import com.houshmandhesab.app.ui.components.PieSlice
import com.houshmandhesab.app.ui.components.SectionHeader
import com.houshmandhesab.app.ui.components.StatTile
import com.houshmandhesab.app.ui.theme.ExpenseRed
import com.houshmandhesab.app.ui.theme.IncomeGreen
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.Jalali
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val wallet: WalletRepository
) : ViewModel() {

    private val _month = MutableStateFlow(wallet.currentMonth())

    private fun monthRange(jy: Int, jm: Int): Pair<Long, Long> =
        Jalali.startOfMonthMillis(jy, jm) to Jalali.endOfMonthMillis(jy, jm)

    val persianDigits = wallet.settings.persianDigits
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val currency = wallet.settings.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val month = _month

    val totals = _month.map { monthRange(it.first, it.second) }
        .flatMapLatest { (from, to) -> wallet.monthTotals(from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryTotals = _month.map { monthRange(it.first, it.second) }
        .flatMapLatest { (from, to) -> wallet.categoryTotals("EXPENSE", from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val txCount = _month.map { monthRange(it.first, it.second) }
        .flatMapLatest { (from, to) -> wallet.transactionsRange(from, to) }
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val biggest = _month.map { monthRange(it.first, it.second) }
        .flatMapLatest { (from, to) -> wallet.transactionDao.biggestExpense(from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val month6 = wallet.last6MonthsTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prevMonth() {
        val (jy, jm) = _month.value
        _month.value = if (jm == 1) jy - 1 to 12 else jy to jm - 1
    }

    fun nextMonth() {
        val (jy, jm) = _month.value
        _month.value = if (jm == 12) jy + 1 to 1 else jy to jm + 1
    }
}

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val categoryTotals by viewModel.categoryTotals.collectAsStateWithLifecycle()
    val txCount by viewModel.txCount.collectAsStateWithLifecycle()
    val biggest by viewModel.biggest.collectAsStateWithLifecycle()
    val month6 by viewModel.month6.collectAsStateWithLifecycle()
    val persian by viewModel.persianDigits.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    val slices = categoryTotals.filter { it.categoryId != null && it.total > 0 }
        .map { PieSlice(it.name ?: "?", it.total.toFloat(), it.color ?: 0xFF94A3B8) }

    val daysInMonth = Jalali.monthLength(month.first, month.second)
    val avgDaily = if (daysInMonth > 0) (totals?.expense ?: 0L) / daysInMonth else 0L

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
                    stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::prevMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                    Text(
                        if (persian) Format.toPersianDigits("${Jalali.monthName(month.second)} ${month.first}")
                        else "${Jalali.monthName(month.second)} ${month.first}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(R.string.this_month_income),
                    value = Format.money(totals?.income ?: 0L, persian, currency),
                    modifier = Modifier.weight(1f),
                    valueColor = IncomeGreen
                )
                StatTile(
                    label = stringResource(R.string.this_month_expense),
                    value = Format.money(totals?.expense ?: 0L, persian, currency),
                    modifier = Modifier.weight(1f),
                    valueColor = ExpenseRed
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(R.string.avg_daily_expense),
                    value = Format.money(avgDaily, persian, currency),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.tx_count),
                    value = if (persian) Format.toPersianDigits(txCount.toString()) else txCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (biggest != null) {
            item {
                StatTile(
                    label = stringResource(R.string.biggest_expense),
                    value = Format.money(biggest!!.amount, persian, currency),
                    modifier = Modifier.fillMaxWidth(),
                    valueColor = ExpenseRed
                )
            }
        }
        item { SectionHeader(stringResource(R.string.by_category)) }
        if (slices.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.no_data_report),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DonutChart(
                            slices = slices,
                            modifier = Modifier
                                .width(170.dp)
                                .height(170.dp),
                            centerLabel = Format.compact(totals?.expense ?: 0L, persian)
                        )
                        PieLegend(slices, persian)
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.monthly_trend)) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                GroupedBarChart(
                    groups = month6.mapIndexed { i, t ->
                        val jm = ((month.second - 5 + i + 12 * 6) % 12).let { if (it == 0) 12 else it }
                        BarGroup(
                            label = Jalali.monthName(jm),
                            income = t.income.toFloat(),
                            expense = t.expense.toFloat()
                        )
                    },
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed
                )
                Spacer(Modifier.height(8.dp))
                ChartLegend(
                    colors = listOf(IncomeGreen, ExpenseRed),
                    labels = listOf(
                        stringResource(R.string.filter_income),
                        stringResource(R.string.filter_expense)
                    )
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
